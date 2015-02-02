package com.iconmaster.srcline;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author iconmaster
 */
public class SLOptions {
	public OutputStream out;
	public InputStream in;
	public OutputStream err;

	public SLOptions(OutputStream out, InputStream in, OutputStream err) {
		this.out = out;
		this.in = in;
		this.err = err;
	}
}
