/*
 * Copyright (C) 2016, 2018 Player, asie
 * Copyright (C) 2021 QuiltMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fabricmc.tinyremapper;

import java.util.regex.Pattern;

public class TinyRemapperConfiguration {
	private boolean removeFrames;
	private boolean ignoreConflicts;
	private boolean resolveMissing;
	private boolean checkPackageAccess;
	private boolean fixPackageAccess;
	private boolean rebuildSourceFilenames;
	private boolean skipLocalVariableMapping;
	private boolean renameInvalidLocals;
	private Pattern invalidLvNamePattern;
	private boolean inferNameFromSameLvIndex;

	public TinyRemapperConfiguration(boolean removeFrames, boolean ignoreConflicts, boolean resolveMissing,
				boolean checkPackageAccess, boolean fixPackageAccess, boolean rebuildSourceFilenames,
				boolean skipLocalMapping, boolean renameInvalidLocals, Pattern invalidLvNamePattern,
				boolean inferNameFromSameLvIndex) {
		this.removeFrames = removeFrames;
		this.ignoreConflicts = ignoreConflicts;
		this.resolveMissing = resolveMissing;
		this.checkPackageAccess = checkPackageAccess;
		this.fixPackageAccess = fixPackageAccess;
		this.rebuildSourceFilenames = rebuildSourceFilenames;
		this.skipLocalVariableMapping = skipLocalMapping;
		this.renameInvalidLocals = renameInvalidLocals;
		this.invalidLvNamePattern = invalidLvNamePattern;
		this.inferNameFromSameLvIndex = inferNameFromSameLvIndex;
	}

	public boolean removeFrames() {
		return this.removeFrames;
	}

	public boolean ignoreConflicts() {
		return this.ignoreConflicts;
	}

	public boolean resolveMissing() {
		return this.resolveMissing;
	}

	public boolean checkPackageAccess() {
		return this.checkPackageAccess || this.fixPackageAccess;
	}

	public boolean fixPackageAccess() {
		return this.fixPackageAccess;
	}

	public boolean rebuildSourceFilenames() {
		return this.rebuildSourceFilenames;
	}

	public boolean skipLocalVariableMapping() {
		return this.skipLocalVariableMapping;
	}

	public boolean renameInvalidLocals() {
		return this.renameInvalidLocals;
	}

	/**
	 * Pattern that flags matching local variable (and arg) names as invalid for the usual renameInvalidLocals processing.
	 */
	public Pattern getInvalidLvNamePattern() {
		return this.invalidLvNamePattern;
	}

	/**
	 * Whether to copy lv names from other local variables if the original name was missing or invalid.
	 */
	public boolean inferNameFromSameLvIndex() {
		return this.inferNameFromSameLvIndex;
	}

	public void setRemoveFrames(boolean removeFrames) {
		this.removeFrames = removeFrames;
	}

	public void setIgnoreConflicts(boolean ignoreConflicts) {
		this.ignoreConflicts = ignoreConflicts;
	}

	public void setResolveMissing(boolean resolveMissing) {
		this.resolveMissing = resolveMissing;
	}

	public void setCheckPackageAccess(boolean checkPackageAccess) {
		this.checkPackageAccess = checkPackageAccess;
	}

	public void setFixPackageAccess(boolean fixPackageAccess) {
		this.fixPackageAccess = fixPackageAccess;
	}

	public void setRebuildSourceFilenames(boolean rebuildSourceFilenames) {
		this.rebuildSourceFilenames = rebuildSourceFilenames;
	}

	public void setSkipLocalVariableMapping(boolean skipLocalVariableMapping) {
		this.skipLocalVariableMapping = skipLocalVariableMapping;
	}

	public void setRenameInvalidLocals(boolean renameInvalidLocals) {
		this.renameInvalidLocals = renameInvalidLocals;
	}

	public void setInvalidLvNamePattern(Pattern invalidLvNamePattern) {
		this.invalidLvNamePattern = invalidLvNamePattern;
	}

	public void setInferNameFromSameLvIndex(boolean inferNameFromSameLvIndex) {
		this.inferNameFromSameLvIndex = inferNameFromSameLvIndex;
	}
}
