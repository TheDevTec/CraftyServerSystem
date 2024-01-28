package me.devtec.craftyserversystem.utils.tablist;

import java.util.List;

public class TablistData {

	private List<String> header;
	private List<String> footer;
	// Tabname
	private String tabNameFormat;
	private String tabPrefix;
	private String tabSuffix;
	// Nametag
	private String tagNameFormat;
	private String tagPrefix;
	private String tagSuffix;
	// Yellow number
	private String yellowNumberPlaceholder;
	private Boolean displayYellowNumberAsInteger;

	@Override
	public boolean equals(Object object) {
		if (object instanceof TablistData)
			return ((TablistData) object).header.equals(header) && ((TablistData) object).footer.equals(footer) && ((TablistData) object).tabNameFormat.equals(tabNameFormat)
					&& ((TablistData) object).tabPrefix.equals(tabPrefix) && ((TablistData) object).tabSuffix.equals(tabSuffix);
		return false;
	}

	public List<String> getHeader() {
		return header;
	}

	public TablistData setHeader(List<String> header) {
		this.header = header;
		return this;
	}

	public List<String> getFooter() {
		return footer;
	}

	public TablistData setFooter(List<String> footer) {
		this.footer = footer;
		return this;
	}

	public String getTabNameFormat() {
		return tabNameFormat;
	}

	public TablistData setTabNameFormat(String tabNameFormat) {
		this.tabNameFormat = tabNameFormat;
		return this;
	}

	public String getYellowNumberText() {
		return yellowNumberPlaceholder;
	}

	public TablistData setYellowNumberText(String yellowNumberPlaceholder) {
		this.yellowNumberPlaceholder = yellowNumberPlaceholder;
		return this;
	}

	public boolean shouldDisplayYellowNumberAsInteger() {
		return displayYellowNumberAsInteger == null ? true : displayYellowNumberAsInteger;
	}

	public TablistData setDisplayYellowNumberAsInteger(boolean displayYellowNumberAsInteger) {
		this.displayYellowNumberAsInteger = displayYellowNumberAsInteger;
		return this;
	}

	public String getTabPrefix() {
		return tabPrefix;
	}

	public TablistData setTabPrefix(String tabPrefix) {
		this.tabPrefix = tabPrefix;
		return this;
	}

	public String getTabSuffix() {
		return tabSuffix;
	}

	public TablistData setTabSuffix(String tabSuffix) {
		this.tabSuffix = tabSuffix;
		return this;
	}

	public String getTagPrefix() {
		return tagPrefix;
	}

	public TablistData setTagPrefix(String tabPrefix) {
		tagPrefix = tabPrefix;
		return this;
	}

	public String getTagSuffix() {
		return tagSuffix;
	}

	public TablistData setTagSuffix(String tabSuffix) {
		tagSuffix = tabSuffix;
		return this;
	}

	public String getTagNameFormat() {
		return tagNameFormat;
	}

	public TablistData setTagNameFormat(String tagNameFormat) {
		this.tagNameFormat = tagNameFormat;
		return this;
	}

	public boolean isComplete() {
		return header != null && footer != null && tabNameFormat != null && tabPrefix != null && tabSuffix != null && yellowNumberPlaceholder != null && displayYellowNumberAsInteger != null
				&& tagNameFormat != null && tagPrefix != null && tagSuffix != null;
	}

	public TablistData fillMissing(TablistData additional) {
		if (header == null)
			header = additional.header;
		if (footer == null)
			footer = additional.footer;
		if (tabNameFormat == null)
			tabNameFormat = additional.tabNameFormat;
		if (tabPrefix == null)
			tabPrefix = additional.tabPrefix;
		if (tabSuffix == null)
			tabSuffix = additional.tabSuffix;
		if (tagNameFormat == null)
			tagNameFormat = additional.tagNameFormat;
		if (tagPrefix == null)
			tagPrefix = additional.tagPrefix;
		if (tagSuffix == null)
			tagSuffix = additional.tagSuffix;
		if (yellowNumberPlaceholder == null)
			yellowNumberPlaceholder = additional.yellowNumberPlaceholder;
		if (displayYellowNumberAsInteger == null)
			displayYellowNumberAsInteger = additional.displayYellowNumberAsInteger;
		return this;
	}
}
