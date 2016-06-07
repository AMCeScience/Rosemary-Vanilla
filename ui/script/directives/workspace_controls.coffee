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

module.directive 'workspaceControls', ->
  restrict: 'E'
  scope: true
  template: """<div id="workspace-control" class="form-group hidden-xs">
      <div class="controls" ng-show="!ctrl.create_workspace_mode">
        <div class="input-group">
          <span class="input-group-addon" ng-click="ctrl.create_workspace_mode = !ctrl.create_workspace_mode"><i class="fa fa-plus-square"></i></span>
          <select class="form-control" ng-model="root.workspace" ng-options="workspace.name for workspace in ctrl.workspaces"></select>
          <span class="input-group-addon" ng-click="root.nav.go('main.workspace')"><i class="fa fa-cog"></i></span>
        </div>
      </div>
      <div class="controls" ng-show="ctrl.create_workspace_mode">
        <div class="input-group">
          <span class="input-group-addon" ng-click="ctrl.create_workspace_mode = !ctrl.create_workspace_mode"><i class="fa fa-cube"></i></span>
          <input type="text" class="form-control" placeholder="Workspace name" ng-model="ctrl.new_workspace">
          <span class="input-group-btn">
            <button class="btn btn-primary" ng-click="ctrl.addWorkspace()">Create!</button>
          </span>
        </div>
      </div>
    </div>"""
  controller: ($scope, $rootScope, $element, Tags, Local, Object, Workspace, Nav) ->

    new class WorkspaceControls extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      workspaces: []
      new_workspace: ""
      create_workspace_mode: false

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

      init: =>
        Tags.workspace_tags.then (@workspaces) =>
          Local.sync 'workspace', null
          $rootScope.workspace =
            if $rootScope.workspace is undefined then @workspaces[0]
            else _.find @workspaces, Object.equalFn $rootScope.workspace

          $rootScope.$watch 'workspace', (n, o) ->
            if not Object.equal n, o
              $rootScope.$emit 'scope_change'

          $rootScope.$on 'scope_change', (n) ->
            Nav.data()

      ###########################
      # Methods                 #
      ###########################
      
      addWorkspace: =>
        Workspace.createWorkspace @new_workspace
        .then (workspace) =>
          Tags.init()
          .then =>
            @workspaces.push workspace
            $rootScope.workspace = workspace
            @new_workspace = ""
            @create_workspace_mode = false