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
module.controller 'MainController', ($scope, $rootScope, $state, Auth, Nav, Tags, Local, Session, Object, Workspace, Websocket) ->

  ctrl = new class MainCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    workspaces: []
    new_workspace: ""
    create_workspace_mode: false
    search_options: SEARCH_OPTIONS

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

    init: =>
      @root.loading = true

      # Default localstorage sync with root scope
      Local.sync 'basket', []
      Object.uniqueAll @root.basket

      @name = Auth.getUser().name
      $rootScope.$on 'scope_change', ->
        $rootScope.ctrl.clearBasket()

      # Call admin functions to fix layout once page is loaded
      $.AdminLTE.layout.fix()
      $.AdminLTE.layout.fixSidebar()

    ###########################
    # Methods                 #
    ###########################

    logout: ->
      Auth.logout()