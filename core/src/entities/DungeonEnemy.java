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
    private final float speed = 40f;
    private boolean markForRemoval = false;
    private boolean isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;

    private List<Vector2> currentPath;
    private int currentPathIndex = 0;
    private float pathUpdateTimer = 0f;
    private final float PATH_UPDATE_INTERVAL = 0.5f; // Recalculate path every 0.5 seconds

    // Stuck detection
    private Vector2 lastPosition = new Vector2();
    private float stuckTimer = 0f;
    private final float STUCK_THRESHOLD = 1.0f; // If stuck for 1 second
    private final float STUCK_DISTANCE = 1f; // Movement less than 1 unit = stuck
    private int pathfindingAttempts = 0;
    private final int MAX_PATHFINDING_ATTEMPTS = 3;

    public DungeonEnemy(Rectangle bounds, Body body, Player player, AnimationManager animationManager, Dungeon dungeon) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.dungeon = dungeon;
        this.lastPosition = new Vector2(body.getPosition());

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

        // Detect if stuck
        checkIfStuck(delta);

        // Update pathfinding timer
        pathUpdateTimer += delta;

        if (isPlayerInRadius()) {
            // Recalculate path periodically or if stuck
            if (pathUpdateTimer >= PATH_UPDATE_INTERVAL || currentPath == null ||
                    currentPath.isEmpty() || stuckTimer >= STUCK_THRESHOLD) {

                if (stuckTimer >= STUCK_THRESHOLD) {
                    pathfindingAttempts++;

                    // If tried pathfinding multiple times and still stuck, give up temporarily
                    if (pathfindingAttempts >= MAX_PATHFINDING_ATTEMPTS) {
                        body.setLinearVelocity(0, 0);
                        currentPath = null;
                        pathfindingAttempts = 0;
                        stuckTimer = 0f;
                        pathUpdateTimer = -2f; // Wait 2 extra seconds before trying again
                        return;
                    }
                }

                calculatePathToPlayer();
                pathUpdateTimer = 0f;

                if (stuckTimer >= STUCK_THRESHOLD) {
                    stuckTimer = 0f;
                }
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
            stuckTimer = 0f;
            pathfindingAttempts = 0;
            if (animationManager.getState("Mushie") != State.IDLE) {
                animationManager.setState(State.IDLE, "Mushie");
                isMoving = false;
            }
        }

        if (isMoving && animationManager.getState("Mushie") != State.RUNNING) {
            animationManager.setState(State.RUNNING, "Mushie");
        }
    }

    private void checkIfStuck(float delta) {
        Vector2 currentPos = body.getPosition();
        float distanceMoved = currentPos.dst(lastPosition);

        // Check if trying to move but not actually moving
        Vector2 velocity = body.getLinearVelocity();
        boolean tryingToMove = velocity.len() > 5f; // Has velocity applied

        if (tryingToMove && distanceMoved < STUCK_DISTANCE * delta) {
            // Not moving enough despite having velocity - definitely stuck
            stuckTimer += delta;
        } else {
            // Moving normally or not trying to move
            stuckTimer = 0f;
            pathfindingAttempts = 0; // Reset attempts when moving successfully
        }

        lastPosition.set(currentPos);
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
        if (distanceToNode < 10f) { // Larger threshold to move to next waypoint faster
            currentPathIndex++;
            if (currentPathIndex >= currentPath.size()) {
                body.setLinearVelocity(0, 0);
                return;
            }
            targetNode = currentPath.get(currentPathIndex);
        }

        // Move towards target node with damping to prevent overshooting
        Vector2 direction = new Vector2(targetNode.x - currentPos.x,
                targetNode.y - currentPos.y);

        float distance = direction.len();
        direction.nor();

        // Slow down when close to waypoint
        float actualSpeed = speed;
        if (distance < 20f) {
            actualSpeed = speed * (distance / 20f);
            actualSpeed = Math.max(actualSpeed, speed * 0.3f); // Minimum 30% speed
        }

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;
        body.setLinearVelocity(direction.scl(actualSpeed));
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