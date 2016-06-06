module = angular.module 'Rosemary'

module.filter 'log', ->
  (data) -> console.log data

module.filter 'print', ->
  (data) -> JSON.stringify data