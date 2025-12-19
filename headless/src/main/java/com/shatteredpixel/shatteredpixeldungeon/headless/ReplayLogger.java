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

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Logs game state and actions for replay and training purposes.
 * Outputs in a canonical format that can be used for:
 * - Determinism verification
 * - Training data collection
 * - Replay visualization
 */
public final class ReplayLogger {
	
	private final RunConfig config;
	private final List<ReplayEntry> entries = new ArrayList<>();
	private final String buildId;
	
	public ReplayLogger(RunConfig config) {
		this.config = config;
		// Get build ID from game version
		this.buildId = String.valueOf(ShatteredPixelDungeon.v3_3_0); // Use latest version constant
	}
	
	/**
	 * Logs a turn with action and state.
	 */
	public void logTurn(int turn, HeadlessAction action, DeterministicRunner.StepResult result) {
		entries.add(new ReplayEntry(turn, action, result));
	}
	
	/**
	 * Writes the replay log to a string.
	 */
	public String toReplayString() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		// Header
		pw.println("# SPD Headless Replay Log");
		pw.println("# Format: turn,action,stateHash,heroX,heroY,heroHP,heroMaxHP,depth,gold,alive");
		pw.println("# Build ID: " + buildId);
		pw.println("# Seed: " + config.seed);
		pw.println("# Hero Class: " + config.heroClass);
		pw.println("# Challenges: " + config.challenges);
		pw.println("# Generated: " + getTimestamp());
		pw.println();
		
		// Entries
		for (ReplayEntry entry : entries) {
			DeterministicRunner.GameState state = entry.result.state;
			pw.printf("%d,%s,%d,%d,%d,%d,%d,%d,%d,%s%n",
				entry.turn,
				entry.action.name(),
				entry.result.stateHash,
				state.heroX,
				state.heroY,
				state.heroHP,
				state.heroMaxHP,
				state.depth,
				state.gold,
				state.alive
			);
		}
		
		pw.flush();
		return sw.toString();
	}
	
	/**
	 * Writes the replay log to stdout.
	 */
	public void printReplay() {
		System.out.print(toReplayString());
	}
	
	private String getTimestamp() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(new Date());
	}
	
	private static class ReplayEntry {
		final int turn;
		final HeadlessAction action;
		final DeterministicRunner.StepResult result;
		
		ReplayEntry(int turn, HeadlessAction action, DeterministicRunner.StepResult result) {
			this.turn = turn;
			this.action = action;
			this.result = result;
		}
	}
}
