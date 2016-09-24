import random

alertCount = 80
reportHeader = 	'<?xml version="1.0"?><OWASPZAPReport version="Dev Build" generated="vie, 9 sep 2016 15:28:57"><site name="http://localhost:9090" host="localhost" port="9090" ssl="false"><alerts>'
reportAlertItem = '<alertitem><pluginid>10012</pluginid><alert>$name</alert><name>$name</name><riskcode>$riskCode</riskcode><confidence>2</confidence><riskdesc>Low (Medium)</riskdesc><desc>&lt;p&gt;AUTOCOMPLETE attribute is not disabled in HTML FORM/INPUT element containing password type input.  Passwords may be stored in browsers and retrieved.&lt;/p&gt;</desc><instances><instance><uri>http://localhost:9090</uri><method>GET</method><param>input</param><evidence>&lt;input required type=&quot;password&quot; class=&quot;form-control&quot; id=&quot;loginpassword&quot; name=&quot;password&quot;&gt;</evidence></instance><instance><uri>http://localhost:9090/login?prevURI=</uri><method>GET</method><param>input</param><evidence>&lt;input required type=&quot;password&quot; class=&quot;form-control&quot; id=&quot;loginpassword&quot; name=&quot;password&quot;&gt;</evidence></instance></instances><count>2</count><solution>&lt;p&gt;Turn off AUTOCOMPLETE attribute in form or individual input elements containing password by using AUTOCOMPLETE=&apos;OFF&apos;&lt;/p&gt;</solution><reference>&lt;p&gt;http://msdn.microsoft.com/library/default.asp?url=/workshop/author/forms/autocomplete_ovr.asp&lt;/p&gt;</reference><cweid>525</cweid></alertitem>'
reportFinish = '</alerts></site></OWASPZAPReport>'
reportAlerts = ''
for i in range(0, alertCount):
	reportAlerts = reportAlerts + reportAlertItem.replace('$riskCode', str(random.randrange(6))).replace('$name','Generated' + str(random.randrange(1000000)))
reportFile = open("generated.xml","w")
reportFile.write(reportHeader + reportAlerts + reportFinish)
reportFile.close()