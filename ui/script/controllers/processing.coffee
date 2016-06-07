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
module.controller 'ProcessingController', ($scope, $rootScope, $q, $state, $stateParams, User, Remote, Object, Tags, Auth) ->

  new class ProcessingCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    @processing = 'noh';

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
      query = Remote.get 'processings/' + $state.params.id
      .then (@processing) =>
        $rootScope.breadcrumb = [
          'Data'
          'Processing'
          @processing.name
        ]

        @last_status = _.last(@processing.statuses).status

        @getIO()
        @getParent()
        @getRecipes()

        $scope.processing = @processing

    getRecipes: =>
      ids = _.map @processing.recipes, (recipe) -> Object.toId recipe
      
      Remote.query 'recipes/query/ids', ids: ids
      .then (data) =>
        if data.length > 0
          # Build Index
          index = {}
          _.forEach data, (d) -> index[Object.toId d] = d

          _.forEach @processing.recipes, (recipe, key) =>
            @processing.recipes[key] = index[Object.toId recipe]

    getIO: =>
      Remote.post Object.toId(@root.workspace) + '/data/query/ids',
        ids: _.map @processing.inputs, (obj) -> Object.toId obj.datum.datum
      .then (@inputs) =>

      Remote.post Object.toId(@root.workspace) + '/data/query/ids',
        ids: _.map @processing.outputs, (obj) -> Object.toId obj.datum.datum
      .then (@outputs) =>

    getParent: =>
      Remote.get 'processing-groups/' + Object.toId @processing.parentId
      .then (@parent) =>

    abort: =>
      Remote.post 'processings/' + $state.params.id + '/action/abort', {}
      .then (response) =>
        @getData()

    resume: =>
      Remote.post 'processings/' + $state.params.id + '/action/resume', {}
      .then (response) =>
        @getData()