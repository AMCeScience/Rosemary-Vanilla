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
module.controller 'ProcessingGroupController', ($scope, $rootScope, $q, $state, $stateParams, User, Remote, Object, Tags, Auth) ->

  new class ProcessingGroupCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

    init: =>
      @root.$watch 'workspace', @getData()
          
    ###########################
    # Methods                 #
    ###########################

    getData: =>
      Remote.get 'processing-groups/' + $state.params.id
      .then (@processing_group) =>
        $rootScope.breadcrumb = [
          'Data'
          'Processing Group'
          @processing_group.name
        ]

        @last_status = _.last(@processing_group.statuses).status

        @getIO()
        @getRecipes()
        @getChildren()

    getChildren: =>
      Remote.get 'processing-groups/' + $state.params.id + '/children'
      .then (@children) =>

    getRecipes: =>
      ids = _.map @processing_group.recipes, (recipe) -> Object.toId recipe
      
      Remote.query 'recipes/query/ids', ids: ids
      .then (data) =>
        if data.length > 0
          # Build Index
          index = {}
          _.forEach data, (d) -> index[Object.toId d] = d

          _.forEach @processing_group.recipes, (recipe, key) =>
            @processing_group.recipes[key] = index[Object.toId recipe]

    getIO: =>
      ids = _.compact _.map @processing_group.inputs, (obj) ->  
        if obj.datum?
          Object.toId obj.datum.datum

      if ids.length > 0
        Remote.post Object.toId(@root.workspace) + '/data/query/ids',
          ids: ids
        .then (@inputs) =>

    abort: =>
      Remote.post 'processing-groups/' + $state.params.id + '/action/abort', {}
      .then (response) =>
        @getData()

    resume: =>
      Remote.post 'processing-groups/' + $state.params.id + '/action/resume', {}
      .then (response) =>
        @getData()