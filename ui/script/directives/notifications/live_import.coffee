class LiveImport

  counts: {}

  constructor: (data) ->
    @id = data.id
    @update data

  update: (data) =>
    @state = data.state
    @updated = moment()

    if @state is "running"
      @counts[data.type] =
        if _.has @counts, data.type then @counts[data.type] + 1
        else 1

module.directive 'livenotificationimport', ->
  restrict: 'E'
  scope:
    data: '='
  template: """
    <a ng-if="data.state != 'complete'">
      <i class="menu-icon fa fa-cloud-download bg-light-blue"></i>
      <div class="menu-info">
        <h4 class="control-sidebar-subheading">Import progressing...</h4>
        <p ng-if="data.state == 'start'">A new import has been started</p>
        <p ng-if="data.state == 'running'">{{data.counts | plural | toSentence | capitalize}} have been imported</p>
        <p>{{data.updated | fromnow}}</p>
      </div>
    </a>
  """