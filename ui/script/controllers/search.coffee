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
module.controller 'SearchController', ($scope, $rootScope, $state, $stateParams, Remote, Tags, Data, Session, Object) ->

  new class SearchCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    data: []
    loading: true
    page: 1
    final: false
    mode: MODE_DEFAULT
    search_options: SEARCH_OPTIONS

    filter_tag_model: ""
    filter_tag_suggestions: []

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

    init: =>
      @root.breadcrumb = [
        'Data'
        'Filter'
      ]

      # Destroy old watchers
      if @root.workspaceWatch?
        @root.workspaceWatch()

      if @root.searchWatch?
        @root.searchWatch()

      @root.workspaceWatch = @root.$watch "workspace", @filter
      @root.searchWatch = @root.$watchCollection "search", @filter

      @filter()
      
      $rootScope.$on 'tag_change', =>
        @initBloodhound()

      @initBloodhound()

    ###########################
    # Methods                 #
    ###########################

    initBloodhound: =>
      Tags.bloodhound_all().then (bloodhound) =>
        @scope.$watch "ctrl.filter_tag_model", (query) =>
          bloodhound.get query, (@filter_tag_suggestions) =>

    addSearchTag: (tag) =>
      @root.search.tags.push tag
      @filter()

    delete: (rm_tag) =>
      @root.search.tags = @root.search.tags.filter (tag) -> !Object.equal tag, rm_tag
      @filter()

    filter: =>
      if @root.workspace?
        @page = 1
        @loading = true
        @final = false

        if $.inArray(@root.workspace, @root.search.tags) == -1
          @root.search.tags.push @root.workspace

        params = 
          query: @root.search.query
          tags: @root.search.tags.map (tag) -> Object.toId tag

        if @root.search.type.id
          params.kind = @root.search.type.id

        changed = not Object.deepEqual params, @params

        if changed
          Remote.query 'query', params
          .then (@data) =>
            @params = params
            @loading = false

    loadMore: =>
      if not @final and @root.workspace? and !@loading
        @loading = true

        @params.page = @page

        Remote.query 'query', @params
        .then (data) =>
          if data.length is 0
            @final = true
          else
            @page = @page + 1
            @data.push.apply @data, data
        .finally =>
          @loading = false

    clear: =>
      @root.search.type = SEARCH_DEFAULT
      @root.search.query = ""
      @root.search.tags = []