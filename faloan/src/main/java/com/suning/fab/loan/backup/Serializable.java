package com.suning.fab.loan.backup;

import java.io.IOException;

public interface Serializable {

	byte[] serialize(final Object obj) throws IOException;

	<T> T deserialize(final byte[] data, Class<T> clazz) throws IOException;

}
