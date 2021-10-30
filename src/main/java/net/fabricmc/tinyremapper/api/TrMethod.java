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

package net.fabricmc.tinyremapper.api;

import org.objectweb.asm.Opcodes;

public interface TrMethod extends TrMember {
	/**
	 * A bridge method, generated by the compiler.
	 */
	default boolean isBridge() {
		return getType().equals(MemberType.METHOD) && (getAccess() & Opcodes.ACC_BRIDGE) != 0;
	}

	/**
	 * Declared native; implemented in a language other than the Java programming language.
	 */
	default boolean isNative() {
		return getType().equals(MemberType.METHOD) && (getAccess() & Opcodes.ACC_NATIVE) != 0;
	}

	/**
	 * Declared abstract; no implementation is provided.
	 */
	default boolean isAbstract() {
		return getType().equals(MemberType.METHOD) && (getAccess() & Opcodes.ACC_ABSTRACT) != 0;
	}

	/**
	 * Non-static and non-private method.
	 */
	default boolean isVirtual() {
		return getType().equals(MemberType.METHOD) && (getAccess() & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0;
	}
}