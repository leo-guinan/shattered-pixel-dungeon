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

package com.shatteredpixel.shatteredpixeldungeon.headless.platform;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.watabou.utils.PlatformSupport;

/**
 * Minimal platform support for headless mode.
 * Disables all UI, audio, and display-related functionality.
 */
public class HeadlessPlatformSupport extends PlatformSupport {
	
	private static HeadlessPlatformSupport instance;
	
	public static void initialize() {
		if (instance == null) {
			instance = new HeadlessPlatformSupport();
			// Set the platform support in Game
			com.watabou.noosa.Game.platform = instance;
		}
	}
	
	public static HeadlessPlatformSupport getInstance() {
		return instance;
	}
	
	@Override
	public void updateDisplaySize() {
		// No-op in headless mode
	}
	
	@Override
	public void updateSystemUI() {
		// No-op in headless mode
	}
	
	@Override
	public boolean connectedToUnmeteredNetwork() {
		return true; // Assume connected in headless mode
	}
	
	@Override
	public boolean supportsVibration() {
		return false; // No vibration in headless mode
	}
	
	@Override
	public void setupFontGenerators(int pageSize, boolean systemFont) {
		// No-op in headless mode - we don't need fonts
		this.pageSize = pageSize;
		this.systemfont = systemFont;
	}
	
	protected FreeTypeFontGenerator getGeneratorForString(String input) {
		return null; // No fonts in headless mode
	}
	
	@Override
	public String[] splitforTextBlock(String text, boolean multiline) {
		// Minimal implementation - just return the text as a single line
		return new String[]{text};
	}
}
