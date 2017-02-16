const SAMPLE_HOSTNAMES = [
	"example.org",
	"foo.bar.com",
	"notawebsite.io",
	"mywebapp.com.ar",
	"mymovies.tv",
	"simpleurl.edu.es"
];

const SAMPLE_TARGETS = [
	"10.54.29.88",
	"10.209.75.32",
	"192.168.84.59",
	"172.16.49.121",
	"192.168.1.22",
	"10.66.16.14"
];

const SEVERITIES = [
	"unclassified",
	"info",
	"low",
	"med",
	"high",
	"critical"
];

const VULN_AMOUNT_MEAN = 10;
const VULN_AMOUN_VAR = 10;

function generateVulnerabilities() {
	let result = [];
	for (i in SAMPLE_HOSTNAMES) {
		SEVERITIES.forEach ( (severity) => {
			let rand = randomIntFromInterval(VULN_AMOUNT_MEAN - VULN_AMOUN_VAR, VULN_AMOUNT_MEAN + VULN_AMOUN_VAR);
			for (let j = 0; j < rand; j++) {
				let vuln = {
					hostnames: [SAMPLE_HOSTNAMES[i]],
					target: SAMPLE_TARGETS[i],
					severity
				};
				result.push(vuln);
			}
		})
	}
	return result;
}

function randomIntFromInterval(a, b) {
    return Math.floor(Math.random() * (b - a + 1) + a);
}