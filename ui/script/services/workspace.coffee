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
module.factory 'Workspace', (Object, User, Tags, Remote) ->

  new class Workspace

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # Constructor & init      #
    ###########################

    ###########################
    # Methods                 #
    ###########################

    createWorkspace: (name) ->
      Remote.put 'tags/workspace-tag', # NEW_API
        name: name

    getMembers: (workspace) ->
      User.users.all().then ->
        _.map workspace.rights.members, User.users.syncGet

    addMember: (workspace, user) ->
      Remote.post 'tags/' + Object.toId(workspace) + '/action/add-members', # NEW_API
        userids: [ Object.toId user ]
      .then (data) -> data[0]

    removeMember: (workspace, user) ->
      Remote.post 'tags/' + Object.toId(workspace) + '/action/remove-members', # NEW_API
        userids: [ Object.toId user ]
      .then (data) -> data[0]

    deleteWorkspace: (workspace) ->
      Remote.delete  'tags/' + Object.toId workspace # NEW_API
      .then -> _.remove Tags.sync_workspace_tags, Object.equalFn workspace