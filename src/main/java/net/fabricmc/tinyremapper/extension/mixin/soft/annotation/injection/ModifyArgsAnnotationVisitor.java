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

package net.fabricmc.tinyremapper.extension.mixin.soft.annotation.injection;

import java.util.List;

import org.objectweb.asm.AnnotationVisitor;

import net.fabricmc.tinyremapper.extension.mixin.common.data.Annotation;
import net.fabricmc.tinyremapper.extension.mixin.common.data.CommonData;

public class ModifyArgsAnnotationVisitor extends CommonInjectionAnnotationVisitor {
	public ModifyArgsAnnotationVisitor(CommonData data, AnnotationVisitor delegate, boolean remap, List<String> targets) {
		super(Annotation.MODIFY_ARGS, data, delegate, remap, targets);
	}
}
