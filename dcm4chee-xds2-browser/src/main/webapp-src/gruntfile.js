module.exports = function (grunt) {

    // load grunt tasks automatically
    require('load-grunt-tasks')(grunt);


    // configurable paths for the application
    var appConfig = {
        indexPath : 'root/index.html',
        srcPath: 'root',
        distPath: './dist'
    };

    grunt.initConfig({

        // cleanup folders
        clean: {
            dist: {
                files: [{
                    dot: true,
                    src: [
                        '.tmp',
                        appConfig.distPath+'/{,*/}*'
                    ]
                }]
            }
        },

        // automatically inject bower components (e.g. javascript dependencies) into the app
        wiredep: {
            app: {
                src: [appConfig.indexPath],
                ignorePath:  /\.\.\//
            }
        },

        // automatically copy 3rd-party dependencies (e.g. javascript libraries) into '/lib' directory
        wiredepCopy: {
            target: {
                options: {
                    src: appConfig.srcPath,
                    dest: appConfig.distPath,
                    wiredep: {
                        cwd:'.'
                    }
                }
            }
        },

        // Copies remaining files to places other tasks can use
        copy: {
            dist: {
                files: [{
                    expand: true,
                    dot: true,
                    cwd: appConfig.srcPath,
                    dest: appConfig.distPath,
                    src: ['**/*', '!lib/**/*', '!**/.gitignore', '!**/nice.*.css']
                }, { // bootstrap fonts
                    expand: true,
                    cwd: appConfig.srcPath,
                    src: 'lib/bootstrap/dist/fonts/**/*',
                    dest: appConfig.distPath
                }]
            }
        },

        useminPrepare: {
            options: {
                dest: 'dist'
            },
            html: appConfig.indexPath
        },
        usemin: {
            html: [appConfig.indexPath]
        }
    });

    grunt.registerTask('cleanup', [
        'clean:dist'
    ]);

    grunt.registerTask('build', [
        'clean:dist',
        'wiredep',
        'wiredepCopy',
        'copy:dist'
    ]);

    grunt.registerTask('default', [
        'build'
    ]);


};