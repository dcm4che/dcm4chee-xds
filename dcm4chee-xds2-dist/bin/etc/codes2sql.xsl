<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">
	<xsl:output method="text" />

	<xsl:template match="/Codes">
		<xsl:apply-templates select="CodeType[@name!='mimeType']" />
	</xsl:template>

	<xsl:template match="CodeType">
		<xsl:apply-templates select="Code" />
	</xsl:template>

	<xsl:template match="Code">
		<xsl:text>INSERT INTO xds_code (classification, meaning, value, designator) VALUES('</xsl:text>
		<xsl:value-of select="../@classScheme" />
		<xsl:text>', '</xsl:text>
		<xsl:value-of select="@display" />
		<xsl:text>', '</xsl:text>
		<xsl:value-of select="@code" />
		<xsl:text>', '</xsl:text>
		<xsl:value-of select="@codingScheme" />
		<xsl:text>');
    </xsl:text>
	</xsl:template>

</xsl:stylesheet>
