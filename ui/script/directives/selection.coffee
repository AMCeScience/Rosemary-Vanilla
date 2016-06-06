module = angular.module 'Rosemary'

module.directive 'selection', ->
  restrict: 'E'
  scope:
    selected: '='
  controller: ($scope, $rootScope, Object) ->

    @instance = new class Selection extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      selectables: []
      last_selected: null

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope

        @scope.$watch "selected", @watchUpdate

      ###########################
      # Methods                 #
      ###########################

      add:    (data) => @scope.selected = _.union @scope.selected, data
      remove: (data) => @scope.selected = _.difference @scope.selected, data

      addSelectable: (selectable) =>
        @selectables.push selectable
        selectable.selected = _.any @scope.selected, Object.equalFn selectable.scope.datum

      watchUpdate: (val, old) =>
        _.forEach @selectables, (s) =>
          s.setSelected _.any @scope.selected, Object.equalFn s.scope.datum

      normalSelect: (@last_selected) =>
        if @last_selected.selected
          @add [ @last_selected.scope.datum ]
        else
          @remove [ @last_selected.scope.datum ]

      shiftSelect: (selected) =>
        last_idx = @selectables.indexOf @last_selected
        selected_idx = @selectables.indexOf selected

        begin = Math.min last_idx, selected_idx
        end = Math.max last_idx, selected_idx

        res = []

        for i in [begin .. end]
          @selectables[i].setSelected true
          res.push @selectables[i].scope.datum
        
        @add res

module.directive 'selectable', ->
  restrict: 'E'
  require: '^selection'
  scope:
    datum: '='
  link: (scope, element, attrs, selection) ->
    scope.ctrl.setSelection selection.instance
    selection.instance.addSelectable scope.ctrl
  controller: ($scope) ->

    new class Selectable extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      selection: null
      selected: false

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope

      ###########################
      # Methods                 #
      ###########################

      click: (e) =>
        if e.shiftKey
          @shiftToggle()
          document.getSelection().removeAllRanges()
        else
          @normalToggle()

        e.stopPropagation()

      shiftToggle: =>
        @selection.shiftSelect @

      normalToggle: =>
        @setSelected not @selected
        @selection.normalSelect @

      setSelection: (@selection) =>

      setSelected: (@selected) =>