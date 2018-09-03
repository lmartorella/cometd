module.exports = function(grunt) {

    var concatList = [
        "cometd-javascript/umd/cometd-header.js",
        "cometd-javascript/common/src/main/webapp/js/cometd/cometd.js",
        "cometd-javascript/common/src/main/webapp/js/cometd/AckExtension.js",
        "cometd-javascript/common/src/main/webapp/js/cometd/TimeSyncExtension.js",
        "cometd-javascript/common/src/main/webapp/js/cometd/BinaryExtension.js"
    ];

	grunt.initConfig({
		concat: {
			cometd: {
				src: concatList,
				dest: "dist/temp/cometd-all-concat.js"
			}
		},
		umd: {
			cometd: {
				options: {
					src: "dist/temp/cometd-all-concat.js",
					dest: "dist/cometd-all.js",
					objectToExport: "org.cometd",
					amdModuleId: 'cometd',
					globalAlias: 'cometd'
				}
			}
		},
		uglify: {
			cometd: {
				files: {
					"dist/cometd-all-min.js": ["dist/cometd-all.js"]
				}
			}
		}
	});

    grunt.loadNpmTasks("grunt-contrib-concat");
    grunt.loadNpmTasks("grunt-contrib-uglify");
    grunt.loadNpmTasks("grunt-umd");
    grunt.registerTask("build", ["concat", "umd", "uglify"]);
};
