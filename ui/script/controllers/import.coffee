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
module.controller 'ImportController', ($scope, $rootScope, Remote, $state, Object, Auth) ->

  new class ImportCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    resources: []
    resource: null
    projects: []
    project: null

    loading: false

    changes: []

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

      @root.breadcrumb = [ 'Import' ]

    init: =>
      Remote.all 'resources' # NEW_API
      .then (@resources) => 

    ###########################
    # Methods                 #
    ###########################

    selectResource: (@resource) =>
      @loading = true
      @next()

      get = =>
        Remote
        .post 'import/get-projects',
          resourceid: Object.toId @resource
        .then (@projects) =>
          @loading = false
  
      if @resource.changed
        Remote
        .post 'users/' + Auth.getUserId() + '/action/add-credential', 
          resource: Object.toId @resource
          username: @resource.username
          password: @resource.password
        .then get
      else get()

    selectproject: (@project) =>
      @next()

    import: ->
      Remote
      .post 'import/get-data',
        resourceid: Object.toId @resource
        workspace: Object.toId @root.workspace
        projecturi: @project.pathOnResource
      .then ->
        $rootScope.nav.data()

    change: (resource) -> resource.changed = true