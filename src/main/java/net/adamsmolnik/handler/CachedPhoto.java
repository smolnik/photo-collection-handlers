package net.adamsmolnik.handler;

import java.io.InputStream;

/**
 * @author asmolnik
 *
 */
class CachedPhoto {

	String fileName;

	InputStream is;

	CachedPhoto(String fileName, InputStream is) {
		this.fileName = fileName;
		this.is = is;
	}

}
