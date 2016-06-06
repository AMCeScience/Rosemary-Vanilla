module = angular.module 'Rosemary'
module.factory 'Tags', ($rootScope, $q, Local, Auth, Object, Tools, Remote) ->

  new class Tags

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # Constructor & init      #
    ###########################

    constructor: ->
      @init()

    init: =>
      @access_tags = Remote
      .get "tags/query/access"
      @category_tags = Remote
      .all "tags/datum-categories"
      @processing_category_tags = Remote
      .all "tags/processing-categories"
      @processing_status_tags = Remote
      .all "tags/processing-status-tags"
      @owner_tags = @access_tags
      .then (r) => r.filter @isOwner
      @user_tags = @access_tags
      .then (r) => r.filter @isUser
      @workspace_tags = @access_tags
      .then (r) => r.filter @isWorkspace
      @tags = $q
      .all [ @access_tags, @category_tags, @processing_category_tags, @processing_status_tags, @owner_tags, @user_tags, @workspace_tags ]
      .then (r) =>
        @sync_access_tags = r[0]
        @sync_category_tags = r[1]
        @sync_processing_category_tags = r[2]
        @sync_processing_status_tags = r[3]
        @sync_owner_tags = r[4]
        @sync_user_tags = r[5]
        @sync_workspace_tags = r[6]

        @sync_tags = @sync_access_tags.concat @sync_category_tags.concat @sync_processing_category_tags.concat @sync_processing_status_tags

        $rootScope.$broadcast 'tag_change'

        @sync_tags

    ###########################
    # Methods                 #
    ###########################

    dataHasTag: (data, tag) ->
      _.any data.tags, Object.equalFn tag

    isWorkspace: (t) -> t._t is TAG_WORKSPACE_TYPE
    isCategory: (t) -> t._t is TAG_CATEGORY_TYPE
    isProcessingCategory: (t) -> t._t is TAG_PROCESSING_CATEGORY_TYPE
    isProcessingStatus: (t) -> t._t is TAG_PROCESSING_STATUS_TYPE
    isDatumCategory: (t) -> t._t is TAG_DATUM_CATEGORY_TYPE
    isUser: (t) -> t._t is TAG_USER_TYPE
    isMessage: (t) -> t._t is TAG_MESSAGE_TYPE

    syncIsWorkspace: (t) => @isWorkspace @syncGet t
    syncIsCategory: (t) => @isCategory @syncGet t

    findType: (o) =>
      for id in o.tags
        tag = @sync_tags.find (tag) -> Object.equal tag, id
        
        if tag?
          if @isDatumCategory tag
            return tag.name.toLowerCase()
          else if @isProcessingCategory tag
            return tag.name.toLowerCase()

    tag: (tag, data, complete) ->
      dataids = _ data
      .filter (datum) -> Object.toType(datum) == 'datum'
      .map Object.toId
      .value()

      processingids = _ data
      .filter (processing) -> Object.toType(processing) == 'processing'
      .map (processing) -> Object.toId processing
      .value()

      processinggroupids = _ data
      .filter (processing_group) -> Object.toType(processing_group) == 'processing_group'
      .map (processing_group) -> Object.toId processing_group
      .value()

      Remote
      .post "tags/action/tag-entities",
        tagid: Object.toId tag
        dataids: dataids
        processingids: processingids
        processinggroupids: processinggroupids
      .then complete

    create: (name, complete) =>
      Remote
      .put "tags/user-tag",
        name: name
      .then (newTags) =>
        @owner_tags.then (tags) -> tags.push newTags
        @user_tags.then (tags) -> tags.push newTags
        @sync_owner_tags.push newTags
        @sync_tags.push newTags

        $rootScope.$broadcast 'tag_change'
      
        complete newTags

    syncGet: (search) =>
      @sync_tags.find (match) ->
        Object.equal match, search

    get: (id) =>
      @tags.then (response) ->
        tag = response.find (tag) -> Object.equal tag, id
        if tag then tag
        else $q.reject()

    isOwner: (tag) ->
      if tag.rights.owner? then Object.equal tag.rights.owner, Auth.getUser()
      else false

    bloodhound: (tags) ->
      tags.then (response) ->
        Tools.bloodhound response

    bloodhound_all: => @bloodhound @tags
    bloodhound_user: => @bloodhound @user_tags

    ###########################
    # TEMP Methods            #
    ###########################

    syncCategory: (ct_name) =>
      _.find @sync_category_tags, (category) -> category.name is ct_name

    syncScan: => @syncCategory 'Scan'
    syncResource: => @syncCategory 'Resource'
    syncReconstruction: => @syncCategory 'Reconstruction'

    syncHasVisualize: (datum) =>
      tag = @syncScan()
      vis = tag? and @dataHasTag datum, tag
      if vis then return true

      tag = @syncResource()
      vis = tag? and @dataHasTag datum, tag
      if vis then return true

      tag = @syncReconstruction()
      tag? and @dataHasTag datum, tag