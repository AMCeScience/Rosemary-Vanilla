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
module.controller 'ProcessController', ($scope, $rootScope, $state, Programs, Remote, Object, Nav) ->

  new class ProcessCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    programs = []
    program = []
    select = null
    description = ""

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

      @scope.$watch 'ctrl.program', => @select = @program?[0]

      Programs.all.then (programs) =>
        @programs = _ programs
        .forEach (program) ->
          program.iParamPort = _.filter program.iPorts, { kind: 'Param' }
          program.iDataPort = _.filter program.iPorts, { kind: 'Data' }
        .groupBy (program) -> program.name
        .value()
        @program = @programs[_.keys(@programs)[0]]

      $rootScope.breadcrumb = [
        'Data'
        'Process'
      ]

    ###########################
    # Methods                 #
    ###########################

    submit: =>
      Remote.put 'processing-groups',
        workspace: Object.toId @root.workspace
        application: Object.toId @select
        description: @description
        dataPorts: _.map @select.iDataPort, (p) =>
          port: p.name
          data: _.map @root.basket, Object.toId
        paramPorts: _.map @select.iParamPort, (p) ->
          port: p.name
          params: [ p.value ]
      .then (data) =>
          Nav.processingGroup data
