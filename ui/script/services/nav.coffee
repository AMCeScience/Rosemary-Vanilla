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

module = angular.module 'Rosemary'
module.factory 'Nav', ($state, $rootScope, Object) ->

  new class Nav

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # Constructor & init      #
    ###########################

    ###########################
    # Methods                 #
    ###########################

    go: (id) ->
      $state.go id

    search: ->
      $state.go 'main.search'

    data: (tag) ->
      $rootScope.search.query = ""
      $rootScope.search.type = SEARCH_OPTIONS[3]
      $rootScope.search.tags = if tag? then [ tag ] else []
      $state.go 'main.search'

    datum: (datum) ->
      $state.go 'main.datum',
        workspaceId: Object.toId $rootScope.workspace
        id: Object.toId datum

    processingGroups: (tag) ->
      $rootScope.search.query = ""
      $rootScope.search.tags = if tag? then [ tag ] else []
      $rootScope.search.type = SEARCH_OPTIONS[2]
      $state.go 'main.search'

    processings: (tag) ->
      $rootScope.search.query = ""
      $rootScope.search.tags = if tag? then [ tag ] else []
      $rootScope.search.type = SEARCH_OPTIONS[1]
      $state.go 'main.search'

    processingGroup: (group) ->
      $state.go 'main.processingGroup',
        id: Object.toId group

    processing: (processing) ->
      $state.go 'main.processing',
        id: Object.toId processing