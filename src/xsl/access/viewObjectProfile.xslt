<?xml version="1.0" encoding="UTF-8"?>
<?xmlspysamplexml C:\mellon\src\xsl\access\getObjectProfile.xml?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" indent="yes"/>
	<xsl:template match="objectProfile">
		<html>
			<head>
				<title>Object Profile HTML Presentation</title>
			</head>
			<body>
				<center>
					<table width="784" border="0" cellpadding="0" cellspacing="0">
						<tr>
							<td width="141" height="134" valign="top">
								<img src="http://www.fedora.info/assets/newlogo2.jpg" width="141" height="134"/>
							</td>
							<td width="643" valign="top">
								<center>
									<h2>Fedora Digital Object</h2>
									<h3>Object Profile View</h3>
								</center>
							</td>
						</tr>
					</table>
					<hr/>
					<xsl:variable name="dissIndex-url">
						<xsl:value-of select="objDissIndexViewURL"/>
					</xsl:variable>
					<a href="{$dissIndex-url}">View the Dissemination Index for this Object</a>
					<p/>
					<xsl:variable name="itemIndex-url">
						<xsl:value-of select="objItemIndexViewURL"/>
					</xsl:variable>
					<a href="{$itemIndex-url}">View the Item Index for this Object</a>
					<hr/>
					<p/>
					<table width="784" border="1" cellpadding="5" cellspacing="5" bgcolor="silver">
					<tr>
						<td align="right">
							<font color="blue">Object Identifier (PID): </font>
						</td>
						<td align="left">
							<xsl:value-of select="@pid"/>
						</td>
					</tr>
					<tr>
						<td align="right">
							<font color="blue">Object Label: </font>
						</td>
						<td align="left">
							<xsl:value-of select="objLabel"/>
						</td>
					</tr>
					<tr>
						<td align="right">
							<font color="blue">Object Content Model: </font>
						</td>
						<td align="left">
							<xsl:value-of select="objContentModel"/>
						</td>
					</tr>
					<tr>
						<td align="right">
							<font color="blue">Object Type: </font>
						</td>
						<td align="left">
							<xsl:value-of select="objType"/>
						</td>
					</tr>
					<tr>
						<td align="right">
							<font color="blue">Object Creation Date: </font>
						</td>
						<td align="left">
							<xsl:value-of select="objCreateDate"/>
						</td>
					</tr>
					<tr>
						<td align="right">
							<font color="blue">Object Last Modified: </font>
						</td>
						<td align="left">
							<xsl:value-of select="objLastModDate"/>
						</td>
					</tr>
					</table>
				</center>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
