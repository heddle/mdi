package edu.cnu.mdi.app;

import javax.swing.Icon;

/** Immutable presentation metadata for an optional application startup window. */
public record StartupInfo(String applicationName, String version, String organization,
		String copyright, Icon logo) {

	public StartupInfo {
		applicationName = text(applicationName);
		version = text(version);
		organization = text(organization);
		copyright = text(copyright);
	}

	public static Builder builder(String applicationName) { return new Builder(applicationName); }

	private static String text(String value) { return value == null ? "" : value.trim(); }

	public static final class Builder {
		private final String applicationName;
		private String version = "", organization = "", copyright = "";
		private Icon logo;
		private Builder(String applicationName) { this.applicationName = applicationName; }
		public Builder version(String value) { version = value; return this; }
		public Builder organization(String value) { organization = value; return this; }
		public Builder copyright(String value) { copyright = value; return this; }
		public Builder logo(Icon value) { logo = value; return this; }
		public StartupInfo build() { return new StartupInfo(applicationName, version, organization, copyright, logo); }
	}
}
