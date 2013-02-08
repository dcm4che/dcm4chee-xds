<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
  
  <xsl:output method="text" />
  <xsl:param name="db" />
   
  <xsl:template match="/Codes">
    <xsl:apply-templates select="CodeType[@name!='mimeType']"/>
  </xsl:template>

  <xsl:template match="CodeType">
       <xsl:apply-templates select="Code"/>
</xsl:template>

  <xsl:template match="Code">
    <xsl:text>INSERT INTO xds_code (</xsl:text>
    <xsl:if test="$db = 'psql'">
    	<xsl:text>pk, </xsl:text>
    </xsl:if>
	<xsl:text>classification, meaning, value, designator) VALUES(</xsl:text>
    <xsl:if test="$db = 'psql'">
    	<xsl:text>nextval('xdscode_pk_seq'), </xsl:text>
    </xsl:if>
    <xsl:text>'</xsl:text> 
	<xsl:value-of select="../@classScheme"/><xsl:text>', </xsl:text>
	<xsl:choose>
		<xsl:when test="$db = 'mysql'">
			<xsl:text>"</xsl:text>
			<xsl:value-of select="@display"/>
			<xsl:text>"</xsl:text>
		</xsl:when>
		<xsl:otherwise>
			<xsl:text>'</xsl:text>
			<xsl:value-of select="replace(@display, '''', '''''')"/>
			<xsl:text>'</xsl:text>
		</xsl:otherwise>	
	</xsl:choose>
	<xsl:text>, '</xsl:text>
    <xsl:value-of select="@code"/><xsl:text>', '</xsl:text>
    <xsl:value-of select="@codingScheme"/><xsl:text>');
    </xsl:text> 
  </xsl:template>

</xsl:stylesheet>
