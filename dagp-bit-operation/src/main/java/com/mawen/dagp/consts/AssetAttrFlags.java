package com.mawen.dagp.consts;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 3.4.2
 */
public interface AssetAttrFlags {

	int CHART = 1; // 0000_0001

	int CHART_MANAGE = CHART << 1; // 0000_0010

	int CHART_ASSOCIATED = CHART << 2;// 0000_0100

	int CHART_LIST = CHART << 3; // 0000_1000

	int CHART_DETAIL = CHART << 4; // 0001_0000

	int ASSOCIATED = 512; // 0000_0010 0000_0000

	int ASSOCIATED_MANAGED = ASSOCIATED << 1; // 0000_0100 0000_0000

	int ASSOCIATED_ASSOCIATED = ASSOCIATED << 2; // 0000_1000 0000_0000

	int ASSOCIATED_LIST = ASSOCIATED << 3; // 0001_0000 0000_0000

	int ASSOCIATED_DETAIL = ASSOCIATED << 4; // 0010_0000 0000_0000

	int getMask();

	default boolean isChartManage() {
		return isChartManage(getMask());
	}

	default boolean isChartManage(int mask) {
		return (mask & CHART_MANAGE) != 0;
	}

	default boolean isChartAssociated() {
		return isChartAssociated(getMask());
	}

	default boolean isChartAssociated(int mask) {
		return (mask & CHART_ASSOCIATED) != 0;
	}

	default boolean isChartList() {
		return isChartList(getMask());
	}

	default boolean isChartList(int mask) {
		return (mask & CHART_LIST) != 0;
	}

	default boolean isChartDetail() {
		return isChartDetail(getMask());
	}

	default boolean isChartDetail(int mask) {
		return (mask & CHART_DETAIL) != 0;
	}

	default boolean isAssociatedAssociated() {
		return isAssociatedAssociated(getMask());
	}

	default boolean isAssociatedAssociated(int mask) {
		return (mask & ASSOCIATED_ASSOCIATED) != 0;
	}

	default boolean isAssociatedManaged() {
		return isAssociatedManaged(getMask());
	}

	default boolean isAssociatedManaged(int mask) {
		return (mask & ASSOCIATED_MANAGED) != 0;
	}

	default boolean isAssociatedList() {
		return isAssociatedList(getMask());
	}

	default boolean isAssociatedList(int mask) {
		return (mask & ASSOCIATED_LIST) != 0;
	}

	default boolean isAssociatedDetail() {
		return isAssociatedDetail(getMask());
	}

	default boolean isAssociatedDetail(int mask) {
		return (mask & ASSOCIATED_DETAIL) != 0;
	}
}
