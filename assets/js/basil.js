import html from "../lib/html-template-tag.js"

window.html = html;

window.basil = function(basil) {
	basil.formatDateTime = function(epochSecond) {
		return moment.unix(epochSecond).format("YYYY.MM.DD hh:mm a");
	};
	basil.formatDate = function(epochSecond) {
		return moment.unix(epochSecond).format("YYYY.MM.DD");
	};
	basil.racecol = function(race) {
		switch (race) {
			case "PROTOSS":
				return "race_protoss";
			case "ZERG":
				return "race_zerg";
			case "TERRAN":
				return "race_terran";
			case "RANDOM":
				return "race_random";
			default:
				return "race_unknown";
		}
	};
	basil.percentFormat = function(value, digits) {
		return new Intl.NumberFormat(undefined, { style: "percent", minimumFractionDigits: digits || 2}).format(value);
	};

	Chart.plugins.unregister(ChartDataLabels);
};

$(function() {window.basil(window.basil);})
