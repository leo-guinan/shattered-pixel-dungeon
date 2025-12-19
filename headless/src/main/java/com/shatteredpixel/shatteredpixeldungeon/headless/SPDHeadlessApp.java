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

import com.badlogic.gdx.ApplicationAdapter;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.headless.platform.HeadlessPlatformSupport;
import com.watabou.noosa.Game;

public final class SPDHeadlessApp extends ApplicationAdapter {
	private final long seed;
	private final HeroClass heroClass;
	private DeterministicRunner runner;
	
	public SPDHeadlessApp(long seed, HeroClass heroClass) {
		this.seed = seed;
		this.heroClass = heroClass;
	}
	
	@Override
	public void create() {
		// Initialize platform support for headless mode
		HeadlessPlatformSupport.initialize();
		
		// Initialize file system for headless mode
		// Use a temporary directory for saves
		String tempDir = System.getProperty("java.io.tmpdir") + "/spd-headless/";
		com.badlogic.gdx.Files.FileType fileType = com.badlogic.gdx.Files.FileType.Absolute;
		com.watabou.utils.FileUtils.setDefaultFileProperties(fileType, tempDir);
		
		// Initialize Game (needed for some core systems)
		// We create a minimal Game instance without scenes
		try {
			// Set version info
			Game.version = "Headless";
			Game.versionCode = ShatteredPixelDungeon.v3_3_0;
			
			// Initialize SPDSettings with headless preferences
			// HeadlessApplication provides getPreferences() method
			com.badlogic.gdx.Preferences prefs = com.badlogic.gdx.Gdx.app.getPreferences("spd-headless");
			com.shatteredpixel.shatteredpixeldungeon.SPDSettings.set(prefs);
			
			// Create and initialize the deterministic runner
			RunConfig config = new RunConfig(seed, heroClass);
			runner = new DeterministicRunner(config);
			runner.startNewGame();
		} catch (Exception e) {
			System.err.println("Error initializing headless game: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	public void render() {
		// In headless mode we DO NOT advance here.
		// The game is driven explicitly via DeterministicRunner.step()
		// This method is called by LibGDX but we ignore it for determinism
	}
	
	public DeterministicRunner getRunner() {
		return runner;
	}
}
