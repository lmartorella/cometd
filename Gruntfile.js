module.exports = function(grunt) {

    var concatList = [
        "cometd-header.js",
        "cometd-javascript/common/src/main/js/org/cometd/CometD.js",
        "cometd-javascript/common/src/main/js/org/cometd/Transport.js",
        "cometd-javascript/common/src/main/js/org/cometd/CallbackPollingTransport.js",
        "cometd-javascript/common/src/main/js/org/cometd/RequestTransport.js",
        "cometd-javascript/common/src/main/js/org/cometd/LongPollingTransport.js",
        "cometd-javascript/common/src/main/js/org/cometd/TransportRegistry.js",
        "cometd-javascript/common/src/main/js/org/cometd/WebSocketTransport.js",
        "cometd-javascript/common/src/main/js/org/cometd/Utils.js",
        "cometd-javascript/common/src/main/webapp/org/cometd/AckExtension.js",
        "cometd-javascript/common/src/main/webapp/org/cometd/TimeSyncExtension.js"
    ];

	grunt.initConfig({
		concat: {
			cometd: {
				src: concatList,
				dest: "cometd-all-concat.js"
			}
		},
		umd: {
			cometd: {
				options: {
					src: "cometd-all-concat.js",
					dest: "cometd-all.js",
					objectToExport: "org.cometd",
					amdModuleId: 'cometd',
					globalAlias: 'cometd'
				}
			}
		},
		uglify: {
			cometd: {
				files: {
					"cometd-all-min.js": ["cometd-all.js"]
				}
			}
		}
	});

    grunt.loadNpmTasks("grunt-contrib-concat");
    grunt.loadNpmTasks("grunt-contrib-uglify");
    grunt.loadNpmTasks("grunt-umd");
    grunt.registerTask("build", ["concat", "umd", "uglify"]);
};
