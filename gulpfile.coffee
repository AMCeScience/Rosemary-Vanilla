# Copyright (C) 2016  Academic Medical Center of the University of Amsterdam (AMC)
#  
# This program is semi-free software: you can redistribute it and/or modify it
# under the terms of the Rosemary license. You may obtain a copy of this
# license at:
# 
# https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the License for the specific language governing permissions and
# limitations under the License.
#  
# You should have received a copy of the Rosemary license
# along with this program. If not, 
# see https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md.
#  
#        Project: https://github.com/AMCeScience/Rosemary-Vanilla
#        AMC eScience Website: http://www.ebioscience.amc.nl/

gulp          = require("gulp-help")(require("gulp"))
util          = require "gulp-util"
notifier      = require "node-notifier"
$             = require("gulp-load-plugins")()

#################################################
# Variables                                     #
#################################################

PATH_BASE_SRC = "ui/"
PATH_BASE_DEST = "public/"

PATH_STYLE = "style/"
PATH_SCRIPT = "script/"

CONCAT_SCRIPT = "app.coffee"

#################################################
# Helper methods                                #
#################################################

notify = (error) ->
  util.log error

  msg =
    if typeof error is "string" then error
    else if error.message? then error.message

  notifier.notify
    title: 'Play-Gulp Error'
    message: msg

  @emit 'end'

#################################################
# LESS                                          #
#################################################

PATH_LESS_SRC_INCLUDE = PATH_BASE_SRC + PATH_STYLE + "**/*.less"
PATH_LESS_SRC_EXCLUSE = '!' + PATH_BASE_SRC + PATH_STYLE + "**/_*.less"
PATH_LESS_SRC_FOLDERS = '!' + PATH_BASE_SRC + PATH_STYLE + "_**/**"
PATH_LESS_SRC         = [ PATH_LESS_SRC_INCLUDE, PATH_LESS_SRC_EXCLUSE, PATH_LESS_SRC_FOLDERS ]
PATH_LESS_DEST        = PATH_BASE_DEST + PATH_STYLE

watch = false

gulp.task "less", "Compile Less -> CSS", ->
  gulp
  .src PATH_LESS_SRC
  .pipe $.sourcemaps.init()
    .pipe $.less()
  .pipe $.sourcemaps.write()
  .on 'error', notify
  .pipe gulp.dest PATH_LESS_DEST
  .pipe $.livereload start: watch

#################################################
# Coffee                                        #
#################################################

PATH_COFEE_SRC = PATH_BASE_SRC + PATH_SCRIPT + "**/*.coffee"
PATH_COFEE_DEST = PATH_BASE_DEST + PATH_SCRIPT

gulp.task "coffee", "Compile Coffee -> JS", ->
  gulp.src PATH_COFEE_SRC
  .pipe $.concat CONCAT_SCRIPT
  .pipe $.sourcemaps.init()
    .pipe $.coffee bare: true
    .on 'error', notify
  .pipe $.sourcemaps.write()
  .pipe gulp.dest PATH_COFEE_DEST
  .pipe $.livereload start: watch

#################################################
# Tasks                                         #
#################################################

wait = (fn) -> setTimeout fn, 1000

gulp.task "_watch", false, -> watch = true

gulp.task "merge", "Git merge frontend & backend into master", $.shell.task [ "source merge.sh" ]

gulp.task "less-watch", "Compile & watch Less -> CSS", [ "_watch", "less" ],      -> gulp.watch PATH_LESS_SRC_INCLUDE, [ "less" ]
gulp.task "coffee-watch", "Compile & watch Coffee -> JS", [ "_watch", "coffee" ], -> gulp.watch PATH_COFEE_SRC, [ "coffee" ]

gulp.task "watch", "Compile & watch all", [ "less-watch", "coffee-watch" ] 
gulp.task "less-build", "Compile Less -> CSS", [ "less" ]
gulp.task "coffee-build", "Compile Coffee -> JS", [ "coffee" ]
gulp.task "default", "Compile all", [ "less", "coffee" ]