package com.iconmaster.srcline;

import com.iconmaster.sbcore.SourceBoxCore;
import com.iconmaster.source.assemble.AssembledOutput;
import com.iconmaster.source.link.Platform;
import com.iconmaster.source.prototype.SourcePackage;

/**
 *
 * @author iconmaster
 */
public class PlatformSourceLine extends Platform {

	public PlatformSourceLine() {
		this.name = "SourceLine";
		
		SourceBoxCore.registerLibs(this);
	}

	@Override
	public boolean canAssemble() {
		return false;
	}

	@Override
	public boolean canRun() {
		return true;
	}

	@Override
	public AssembledOutput assemble(SourcePackage pkg) {
		return new AssembledOutput() {};
	}

	@Override
	public Object run(SourcePackage pkg) {
		return null;
	}
}
