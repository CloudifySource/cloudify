package org.openspaces.cloud.examples;

import java.io.Serializable;
import java.util.Map;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceIndexes;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.gigaspaces.metadata.index.SpaceIndexType;

/*********
 * Simple data class for the test cases.
 * @author barakme
 *
 */
@SpaceClass
public class TestData implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private String data;
	private Map<String, Object> info;

	/**
	 * Constructs a new Data object.
	 */
	public TestData() {
	}

	/**
	 * Constructs a new Data object with the given type and raw data.
	 * @param id .
	 * @param data .
	 * @param info .
	 */
	public TestData(final long id, final String data, final Map<String, Object> info) {
		this.id = id;
		this.data = data;
		this.info = info;
	}

	/**
	 * The processed data this object holds.
	 */
	public String getData() {
		return data;
	}

	/**
	 * The id of this object.
	 */
	@SpaceRouting
	@SpaceId
	public Long getId() {
		return id;
	}

	// This defines several indexes on the same info property
	@SpaceIndexes( {
			@SpaceIndex(path = "info.address", type = SpaceIndexType.BASIC),
			@SpaceIndex(path = "info.socialSecurity", type = SpaceIndexType.BASIC) })
	public Map<String, Object> getInfo() {
		return info;
	}

	/**
	 * The processed data this object holds.
	 * 
	 * @param data the new data.
	 *
	 */
	public void setData(final String data) {
		this.data = data;
	}

	/**
	 * The id of this object. Its value will be auto generated when it is
	 * written to the space.
	 * 
	 * @param id the new id.
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	public void setInfo(final Map<String, Object> info) {
		this.info = info;
	}

	@Override
	public String toString() {
		return "id[" + id + "] data[" + data + "]";
	}
}
