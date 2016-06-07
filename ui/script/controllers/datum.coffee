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
module.controller 'DatumController', ($scope, $rootScope, $state, $stateParams, $q, Remote, Tags, Data, Object, Tools) ->

  new class DatumCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    mode: MODE_DEFAULT
    children: []
    replicas: []
    info_search: ""
    info_search_suggestions: []
    visualize: false

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

    init: =>
      @root.$watch 'workspace', @loadDatum
        

    ###########################
    # Methods                 #
    ###########################

    loadDatum: =>
      if @root.workspace?
        query = Remote.get Object.toId(@root.workspace) + '/data/' + $stateParams.id # NEW_API

        $q.all [ query, Tags.category_tags ]
        .then (result) =>
          @datum = result[0]

          @root.breadcrumb = [
            'Data'
            @datum.name
          ]

          @has_visualize = Tags.syncHasVisualize @datum

          info = _.map @datum.info.dict, (value, key) ->
            key: key
            value: value

          @bloodhound = Tools.bloodhound info, (o) -> o.key + ' ' + o.value
          @scope.$watch "ctrl.info_search", =>
            if _.isBlank @info_search
              @info_search_suggestions = info
            else
              @bloodhound.get @info_search, (@info_search_suggestions) =>

          if @datum.children?.length > 0
            Data.children @root.workspace, @datum
            .then (@children) =>

          @loadParents()

          @getIO()

          Data.resources.all()
          .then (resources) => 
            @replicas = _.forEach @datum.replicas, (replica) =>
              replica.resource = Data.resources.syncGet replica.resource
              replica.resource.url = @resourceToUrl replica.resource

    loadParents: =>
      Data.parents @root.workspace, @datum
      .then (@parents) =>
        @parents = _.forEach @parents, (parent) =>
          parent.type = Tags.findType parent

    resourceToUrl: (resource) ->
      "#{resource.protocol}://#{resource.host}:#{resource.port}#{resource.basePath}"

    getIO: =>
      processing_input_query = Remote.post 'processings/for-io',
        input: Object.toId @datum

      processing_output_query = Remote.post 'processings/for-io',
        output: Object.toId @datum

      group_query = Remote.post 'processing-groups/for-io',
        input: Object.toId @datum

      $q.all [ processing_input_query, processing_output_query, group_query ]
      .then (result) =>
        @processing_inputs = result[0]
        @outputs = result[1]
        @group_inputs = result[2]

        @inputs = _.union @processing_inputs, @group_inputs

        arr = 
          _.union @inputs, @outputs
          .map (d) ->
            d.recipes.map (recipe) -> Object.toId recipe
        
        ids = [].concat.apply([], arr)

        Remote.query 'recipes/query/ids', ids: ids
        .then (data) =>
          if data.length > 0
            # Build Index
            index = {}
            _.forEach data, (d) -> index[Object.toId d] = d

            _.forEach @inputs, (d) ->
              _.forEach d.recipes, (recipe, key) ->
                d.recipes[key] = index[Object.toId recipe]

            _.forEach @outputs, (d) ->
              _.forEach d.recipes, (recipe, key) ->
                d.recipes[key] = index[Object.toId recipe]
        

    ###########################
    # TEMP Methods            #
    ###########################

