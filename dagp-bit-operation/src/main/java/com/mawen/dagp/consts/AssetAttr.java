package com.mawen.dagp.consts;

import lombok.Data;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/3/26
 */
@Data
public class AssetAttr implements AssetAttrFlags {

	private Long id;

	private String name;

	private int mask;

	private boolean canChartManaged;

	private boolean canChartAssociated;

	private boolean canChartList;

	private boolean canChartDetail;

	private boolean canAssociatedManaged;

	private boolean canAssociatedAssociated;

	private boolean canAssociatedList;

	private boolean canAssociatedDetail;
}
