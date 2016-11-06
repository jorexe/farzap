#Farzap - ZAP Faraday Plugin

##Authors

Jorge Gómez - Julieta Sal-lari - Santiago Ramirez Ayuso
Insituto Tecnológico de Buenos Aires

##Extensions

Our plugin consists of two extensions to load alerts into Faraday. Both needs faraday client running.

###XML Export

When active scan is completed, you can export ZAP Report into Faraday in two different ways.

Click on tools bar, and then click "Send Report to Faraday" option.

![Zap Example 1](/doc/img/01.png)

Or select one or more alerts in the alerts panel and right click them. Then coose same option: "Send Report to Faraday".

![Zap Example 2](/doc/img/02.png)

Once option selected, a pop up will apear. Check the faraday report folder where the report it's going to export. By default it's UserHome/.faraday/report.
The plugin will update the combobox based on wich workspaces are in that folder.

![Zap Example 3](/doc/img/03.png)

Then ensure Faraday Zap Plugin importer executed correctly.

![Zap Example 4](/doc/img/04.png)

After that alerts will appear into the workspace.

![Zap Example 5](/doc/img/05.png)

###RPC Export
