/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.headless;

/**
 * Canonical Action Space for Shattered Pixel Dungeon (Headless Mode)
 * 
 * This enum defines the complete set of actions available to an agent playing SPD.
 * Actions are designed to be:
 * - Deterministic: Same action + state = same result
 * - Complete: All game interactions are expressible
 * - Minimal: No redundant actions
 * 
 * ACTION SEMANTICS:
 * 
 * === MOVEMENT ACTIONS (8 directions) ===
 * Movement actions are context-aware and automatically handle:
 * - Attacking: Moving into an enemy cell attacks that enemy
 * - Picking up: Moving onto an item cell picks it up (if no enemies nearby)
 * - Opening chests: Moving onto a chest opens it
 * - Unlocking doors: Moving onto a locked door unlocks it (if hero has key)
 * - Level transitions: Moving onto stairs transitions to next level
 * - Mining: Moving onto a mineable wall mines it (if hero has pickaxe)
 * - Alchemy: Moving onto an alchemy pot uses it
 * 
 * If the target cell is empty/passable, hero simply moves there.
 * If the target cell is blocked, the action is ignored (no-op).
 * 
 * MOVE_N, MOVE_NE, MOVE_E, MOVE_SE, MOVE_S, MOVE_SW, MOVE_W, MOVE_NW
 *   - Directional movement (N=North, E=East, etc.)
 *   - dx/dy: Relative movement offset
 * 
 * === WAIT ===
 * WAIT
 *   - Hero skips turn without moving
 *   - Enemies and buffs still process
 *   - Useful for tactical positioning
 * 
 * === EXPLICIT INTERACTIONS ===
 * PICKUP
 *   - Explicitly pick up item at current position
 *   - Useful when enemies are nearby (auto-pickup disabled)
 * 
 * REST
 *   - Rest until fully healed or interrupted
 *   - Hero will continue resting until HP is full or damage is taken
 *   - More efficient than multiple WAIT actions for healing
 * 
 * === ACTION COUNT ===
 * Total: 11 actions
 * - 8 movement directions
 * - 1 wait
 * - 1 pickup
 * - 1 rest
 * 
 * === USAGE NOTES ===
 * - Movement actions are preferred for most interactions (they're context-aware)
 * - Use PICKUP only when you need explicit pickup (e.g., enemies nearby)
 * - Use REST for healing, WAIT for tactical positioning
 * - Invalid moves (out of bounds, blocked) are silently ignored
 * 
 * === FUTURE EXTENSIONS ===
 * The following actions may be added in future versions:
 * - USE_ITEM(slot): Use item from inventory by slot index
 * - INTERACT(direction): Explicit interaction with NPCs/objects
 * - DESCEND: Explicit level transition (currently auto-handled by movement)
 */
public enum HeadlessAction {
	// === MOVEMENT ACTIONS (8 directions) ===
	// These are context-aware: automatically attack, pickup, open doors, etc.
	MOVE_N(0, -1),      // North
	MOVE_NE(1, -1),     // Northeast
	MOVE_E(1, 0),       // East
	MOVE_SE(1, 1),      // Southeast
	MOVE_S(0, 1),       // South
	MOVE_SW(-1, 1),     // Southwest
	MOVE_W(-1, 0),      // West
	MOVE_NW(-1, -1),    // Northwest
	
	// === WAIT ===
	WAIT(0, 0),         // Skip turn without moving
	
	// === EXPLICIT INTERACTIONS ===
	PICKUP(0, 0),       // Pick up item at current position (explicit)
	REST(0, 0),         // Rest until fully healed or interrupted
	;
	
	/** Relative movement offset (dx, dy) for movement actions */
	public final int dx, dy;
	
	HeadlessAction(int dx, int dy) {
		this.dx = dx;
		this.dy = dy;
	}
	
	/**
	 * Returns true if this is a movement action (has direction).
	 * Movement actions are context-aware and handle interactions automatically.
	 */
	public boolean isMovement() {
		return this != PICKUP && this != REST && this != WAIT;
	}
	
	/**
	 * Returns the total number of actions in the canonical action space.
	 */
	public static int actionCount() {
		return values().length;
	}
	
	/**
	 * Returns a human-readable description of what this action does.
	 */
	public String getDescription() {
		switch (this) {
			case MOVE_N: return "Move North (context-aware: attacks, picks up, opens doors, etc.)";
			case MOVE_NE: return "Move Northeast (context-aware)";
			case MOVE_E: return "Move East (context-aware)";
			case MOVE_SE: return "Move Southeast (context-aware)";
			case MOVE_S: return "Move South (context-aware)";
			case MOVE_SW: return "Move Southwest (context-aware)";
			case MOVE_W: return "Move West (context-aware)";
			case MOVE_NW: return "Move Northwest (context-aware)";
			case WAIT: return "Wait (skip turn without moving)";
			case PICKUP: return "Pick up item at current position (explicit)";
			case REST: return "Rest until fully healed or interrupted";
			default: return "Unknown action";
		}
	}
}
