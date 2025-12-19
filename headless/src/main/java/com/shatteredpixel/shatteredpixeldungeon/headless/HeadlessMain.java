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

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;

public final class HeadlessMain {
	
	public static void main(String[] args) {
		// Parse args: seed, class, maxTurns, output path, etc.
		long seed = Long.parseLong(System.getenv().getOrDefault("SPD_SEED", "12345"));
		String heroClassStr = System.getenv().getOrDefault("SPD_CLASS", "WARRIOR");
		HeroClass heroClass = HeroClass.valueOf(heroClassStr.toUpperCase());
		
		HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
		config.updatesPerSecond = 0; // important: don't drive on wall-clock time
		
		SPDHeadlessApp app = new SPDHeadlessApp(seed, heroClass);
		new HeadlessApplication(app, config);
		
		// The app will run in headless mode
		// Use DeterministicRunner.step() to advance the game
	}
}
