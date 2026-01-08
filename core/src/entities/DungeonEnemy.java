package entities;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import managers.AnimationManager;
import managers.AnimationManager.State;
import managers.Dungeon;

public class DungeonEnemy {
    public Rectangle bounds;
    private Body body;
    private final Player player;
    private final Dungeon dungeon;
    private final float detectionRadius = 150f;
    private final float speed = 60f;
    private boolean markForRemoval = false;
    private boolean isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;

    private List<Vector2> currentPath;
    private int currentPathIndex = 0;
    private float pathUpdateTimer = 0f;
    private final float PATH_UPDATE_INTERVAL = 0.5f; // Recalculate path every 0.5 seconds

    public DungeonEnemy(Rectangle bounds, Body body, Player player, AnimationManager animationManager, Dungeon dungeon) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.dungeon = dungeon;

        animationManager.setState(AnimationManager.State.IDLE, "Mushie");
    }

    public void update(float delta) {
        if (markForRemoval) {
            dispose();
            return;
        }

        if (!Player.gameStarted) {
            body.setLinearVelocity(0, 0);
            return;
        }

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        // Update pathfinding timer
        pathUpdateTimer += delta;

        if (isPlayerInRadius()) {
            // Recalculate path periodically
            if (pathUpdateTimer >= PATH_UPDATE_INTERVAL || currentPath == null || currentPath.isEmpty()) {
                calculatePathToPlayer();
                pathUpdateTimer = 0f;
            }

            // Follow path
            if (currentPath != null && !currentPath.isEmpty()) {
                followPath();
                isMoving = true;
            } else {
                body.setLinearVelocity(0, 0);
                isMoving = false;
            }
        } else {
            body.setLinearVelocity(0, 0);
            currentPath = null;
            currentPathIndex = 0;
            if (animationManager.getState("Mushie") != State.IDLE) {
                animationManager.setState(State.IDLE, "Mushie");
                isMoving = false;
            }
        }

        if (isMoving && animationManager.getState("Mushie") != State.RUNNING) {
            animationManager.setState(State.RUNNING, "Mushie");
        }
    }

    private void calculatePathToPlayer() {
        Vector2 enemyPos = body.getPosition();
        Vector2 playerPos = player.getPosition();

        currentPath = dungeon.findPath(enemyPos, playerPos);
        currentPathIndex = 0;

        // Skip first node (current position)
        if (currentPath != null && currentPath.size() > 1) {
            currentPathIndex = 1;
        }
    }

    private void followPath() {
        if (currentPath == null || currentPathIndex >= currentPath.size()) {
            body.setLinearVelocity(0, 0);
            return;
        }

        Vector2 targetNode = currentPath.get(currentPathIndex);
        Vector2 currentPos = body.getPosition();

        // Check if reached current node
        float distanceToNode = currentPos.dst(targetNode);
        if (distanceToNode < 5f) {
            currentPathIndex++;
            if (currentPathIndex >= currentPath.size()) {
                body.setLinearVelocity(0, 0);
                return;
            }
            targetNode = currentPath.get(currentPathIndex);
        }

        // Move towards target node
        Vector2 direction = new Vector2(targetNode.x - currentPos.x,
                targetNode.y - currentPos.y).nor();

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;
        body.setLinearVelocity(direction.scl(speed));
    }

    public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            TextureRegion currentFrame = new TextureRegion(animationManager.getMushieCurrentFrame());
            currentFrame.flip(isFlipped, false);
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    public void dispose() {
        if (body != null && markForRemoval) {
            body.getWorld().destroyBody(body);
            body = null;
        }
    }

    public void markForRemoval() {
        markForRemoval = true;
    }

    public boolean isMarkedForRemoval() {
        return markForRemoval;
    }

    public Body getBody() {
        return body;
    }

    private boolean isPlayerInRadius() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        float distance = playerPosition.dst(enemyPosition);
        return distance <= detectionRadius;
    }
}
