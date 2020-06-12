<template>
    <div class="home" style="margin-top: 40px">
        <!--    <img alt="Vue logo" src="../assets/logo.png">-->
        <!--    <div>-->
        <!--      <p>-->
        <!--        If Element is successfully added to this project, you'll see an-->
        <!--        <code v-text="'<el-button>'"></code>-->
        <!--        below-->
        <!--      </p>-->
        <!--      <el-button>el-button</el-button>-->
        <!--    </div>-->

        <el-card  class="card-wrapper" style="max-width: 600px;margin: 0 auto; padding: 15px; ">
            <div style="display: flex; color: #999; ">
                <div style="flex-grow: 1; padding-right: 5px; display: flex; align-items: center; ">
                    <span style="font-size: 14px; margin: 0; padding: 0"><i class="el-icon-connection"></i>
                        <template v-if="isConnected">
                            <i class="el-icon-check"></i> 已连接到游戏服务器
                        </template>
                        <template v-else>
                            <span style="color: #F56C6C"><i class="el-icon-close"></i> 未连接到游戏服务器</span>
                        </template>
                        {{ this.gameServer }}
                    </span>
                </div>
                <div>
                    <el-button type="success" round v-if="!isConnected" size="small" @click="connect">连接游戏服务器</el-button>
                    <el-button type="warning" round v-if="isConnected" size="small" @click="disconnect">关闭连接</el-button>
                </div>
            </div>

            <div >
                <h3 style="margin: 0.3em 0; padding: 0"><i class="el-icon-user"></i> 玩家信息</h3>
<!--                <el-avatar>{{ players.self }}</el-avatar>-->
                <div style="display: flex; align-items: baseline; ">
                    <span style="margin-right: 10px">我的昵称: <span style="font-size: 1.5em">{{ players.self }}</span>  </span>
                <el-button type="text" @click="() => {form_nickname.name = players.self; dialogForm_nickname_visible = true}">修改昵称</el-button>

                </div>

                <el-dialog id="dialog-nickname" title="修改我的昵称" :visible.sync="dialogForm_nickname_visible">
                    <el-form :model="form_nickname" :rules="form_nickname_rules" ref="form_nickname" >
                        <el-form-item prop="name">
                        <el-input size="large" v-model="form_nickname.name" :value="players.self" autocomplete="off"
                                  @keypress.enter.native="handleSubmitForm('form_nickname', handleChangeNicknameClick)"></el-input>
                        </el-form-item>
                    </el-form>
                    <div slot="footer" class="dialog-footer">
                        <el-button @click="() => {dialogForm_nickname_visible = false;}">取 消</el-button>
                        <el-button type="primary" @click="handleSubmitForm('form_nickname', handleChangeNicknameClick)"
                                   >确 定</el-button>
                    </div>
                </el-dialog>

            </div>
            <div style="text-align: center; color: #666; margin: 10px 0; padding: 5px 0;">与朋友输入同样的暗号进入同一房间进行游戏</div>
            <template v-if="!isInRoom">




                <el-row v-loading="loading_form_room" :gutter="15">
                    <el-col>
                        <el-form :model="form_room" :rules="form_room_rules" ref="form_room" label-position="left" label-width="85px">
                            <el-col :span="18">
                                <el-form-item label="房间暗号" prop="room_id">
                                    <el-input size="large" placeholder="输入房间暗号" v-model="form_room.room_id" autocomplete="off">
                                    </el-input>
                                </el-form-item>
                            </el-col>
                            <el-col :span="6" >
                                <el-button native-type="submit" style="width: 100%" type="primary" round @click="handleSubmitForm('form_room', handleEnterRoomClick)" >进入游戏房间</el-button>
                            </el-col>
                        </el-form>
                    </el-col>

                </el-row>



            </template>

            <template v-if="isInRoom">
                <div style="text-align: center">当前房间号: <span style="font-size: 1.5em">{{ room_id }}</span></div>
                <div style="padding: 10px 0 15px 0;">
                    <el-button style="width: 100%" type="primary" round @click="handleLeaveRoomClick">离开游戏房间</el-button>
                </div>
            </template>
            <!--  棋盘  -->
            <!--            isInRoom-->
            <div v-if="isInRoom" style="display: flex; flex-direction: column; justify-content: center; ">

                <el-row :gutter="20" type="flex">
                    <el-col :span="12" style="display: flex; align-items: center">
                        <el-avatar style="margin-right: 10px; user-select: none" :class="avatar_class['self']">{{ players.self }}</el-avatar>
                        <div style="display: flex; flex-direction: column">
                            <div>{{ players.self }}</div>
                            <div style="font-size: 0.85em; color: #909399"><template v-if="isInRoom">我方执{{ UI_string_my_color }}棋</template></div>
                        </div>
                    </el-col>
                    <el-col :span="12" style="display: flex; align-items: center">
                        <template v-if="players.opponent !== null">
                            <el-avatar style="margin-right: 10px; user-select: none" :class="avatar_class['opponent']">{{ players.opponent }}</el-avatar>
                            <div style="display: flex; flex-direction: column">
                                <div>{{ players.opponent }} </div>
                                <div style="font-size: 0.85em; color: #909399"><template v-if="isInRoom">对方执{{ UI_string_opponent_color }}棋</template></div>
                            </div>
                        </template>
                        <template v-else>
                            <div style="flex-grow: 1; height: 100%; background-color: #fafafa; display: flex; align-items: center; border-radius: 10px;">
                                <div style="width: 100%; text-align: center; font-size: 14px; color: #666">
                                    <template v-if="isInRoom">
                                        等待玩家加入
                                    </template>
                                    <template v-else>
                                        等待进入房间
                                    </template>
                                </div>
                            </div>
                        </template>

                    </el-col>
                </el-row>

                <el-row style="margin: 0.5em 0; text-align: center">
                    <el-col>
                        <div v-if="false">
                            <template v-if="isInRoom">
                                等待玩家加入
                            </template>
                            <template v-else>
                                等待进入房间
                            </template>
                        </div>
                        <template v-if="isGameStarted && !isGameEnd">
                            <div>游戏进行中</div>
                        </template>
                        <template v-if="isGameStarted">
                            <div>轮到 <span style="font-size: 1.5em">{{ UI_string_whoseTurn }}</span> 掷棋</div>
                            <template v-if="countdown_number > 0">
                                <span style="font-size: 1.5em">{{ countdown_number }}</span> 秒后超时
                            </template>
                            <template v-else>
                                <span style="font-size: 1.5em">掷棋超时</span>
                            </template>
                        </template>
                        <template v-if="isGameEnd">
                            <div>游戏结束！<span style="font-size: 1.5em">{{ UI_string_winner.role }}{{ UI_string_winner.stone }}</span> 获胜！</div>
                            <el-button style="width: 100%; margin: 10px 0;" type="success" round @click="handleAgainClick">再来一局</el-button>
                        </template>
<!--                        <el-button  type="success" round>准备游戏</el-button>-->
                    </el-col>
                </el-row>

                <div>
                    <div class="board-container" :style="{'--self-stone-background': board_css_self_stone_background}">
                        <div :class="board_class[cell_idx]"
                             v-for="(cell, cell_idx) in this.board.stones" :key="cell_idx" @click="handleStoneSlotClick(cell_idx)">
                            <div style="display: flex; justify-content: center; align-items: center">
                                <div></div>
                            </div>
                        </div>
                    </div>
                </div>


                <div v-if="false">
<!--                    <table style="border-collapse: collapse">-->
<!--                        <tr v-for="x in board.size" :key="x">-->
<!--                            <td v-for="y in board.size" :key="y">{{x}} {{y}}</td>-->

<!--                        </tr>-->
<!--                    </table>-->
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>

  /* eslint no-unused-vars: 0 */
  import EventBus from 'vertx3-eventbus-client'

  let eb = null
  let my_uuid = null

  const kBlackStone = 1;
  const kWhiteStone = 2;


  function debug_notify_communication(action, error, message) {
    console.log(`recieved a reply for action <${action}>: `, {error}, {message})
    // if (message) {
    //   UI.$notify({
    //     title: `<${action}> reply 👇`,
    //     dangerouslyUseHTMLString: true,
    //     message: JSON.stringify({error}) + '<br><b>' + JSON.stringify({message}) + '</b>',
    //     duration: 0,
    //     type: 'info'
    //   });
    // } else {
    //   UI.$notify({
    //     title: `<${action}> error ❌`,
    //     dangerouslyUseHTMLString: true,
    //     message: '<b>' + JSON.stringify({error}) + '</b>',
    //     duration: 0,
    //     type: 'error'
    //   });
    // }

  }

  let game_heartbeat_interval = 5000
  let game_heartbeat_timer = null
  // 心跳包（每秒）
  function game__heartbeat() {
    console.log('heartbeat tick')
    const action = 'heart beat'
    if (!eb) {
      game_heartbeat_stop()
    }
    eb.send(`game.player.${my_uuid}.s`, {action: action});  // 无服务器响应
  }

  function game_heartbeat_start() {
    game_heartbeat_stop()
    if (typeof game_heartbeat_interval !== 'number') {
      throw '`game_heartbeat_interval` should be a number'
    }
    game_heartbeat_timer = setInterval(game__heartbeat, game_heartbeat_interval)
    console.log('start heartbeat', game_heartbeat_timer)
  }

  function game_heartbeat_stop() {
    if (game_heartbeat_timer !== null) {
      clearInterval(game_heartbeat_timer)
      game_heartbeat_timer = null
    }
  }

  // 修改昵称（任何时候）
  function game_update_nickname(new_nickname) {
    const action = 'update nickname'
    console.log(action, new_nickname)
    eb.send(`game.player.${my_uuid}.s`, {action: action, name: new_nickname});  // 无服务器响应
  }

  // 进入房间
  function game_enter_room(room_id, callback) {
    const action = 'enter room'
    eb.send(`game.player.${my_uuid}.s`, {action: action, id: room_id}, {}, (error, message) => {
      debug_notify_communication(action, error, message)
      try {
        callback(error, message)

      } catch (e) {
        console.dir(e)
      }
    });
  }

  // 离开房间
  function game_leave_room() {
    const action = 'leave room'
    eb.send(`game.player.${my_uuid}.s`, {action: action});
  }


  // 事件：游戏开局
  // 游戏开始的标志是服务器首次通知自己或对方落子，因此在这两个事件中触发 game__if_trig_start_event 检测是否开局。
  function game_on_game_start() {
    UI.isGameStarted = true
    UI.$message({
      showClose: true,
      message: '游戏开始！',
      type: 'success'
    });
  }

  // 在通知己方或对方落子的事件中被调用，用来判断是否为首次通知落子，首次意味着开局
  function game__if_trig_start_event() {
    if (UI.isGameStarted === true) {
      return
    }
    game_on_game_start()
  }

  function game_on_opponent_nickname_updated(message) {
    // {"action":"opponent nickname","name": "ynyyn"}
    // UI.players.opponent = message.body.name
    const name = message.body.name
    UI.$set(UI.players, 'opponent', name)
  }

  // 事件：轮到对方落子
  function game_on_opponent_to_put_stone(message) {
    // {"action":"opponent to put","timeout":30}
    // check if implies game start
    game__if_trig_start_event()
    // switch turn
    UI.whoseTurn = 'opponent'
    UI.countdown_start(message.body.timeout)
  }

  let _put_stone_reply = null

  // 事件：轮到己方落子
  function game_on_self_to_put_stone(message) {
    // {"action":"to put","timeout":30}
    // check if implies game start
    game__if_trig_start_event()
    // switch turn
    _put_stone_reply = message.reply
    UI.whoseTurn = 'self'
    UI.countdown_start(message.body.timeout)
    // reply in other place: game_put_stone
  }

  // 落子
  function game_put_stone(x, y, callback) {
    // reply
    const action = 'put'
    if (typeof _put_stone_reply !== 'function') {
        throw 'not self turn'
    }
    _put_stone_reply({action, x, y})
    if (typeof callback === 'function') {
      callback(x, y)
    }

    // ui show stone
    UI.set_stone_self(x, y)
    _put_stone_reply = null
    // UI.whoseTurn = null
  }

  // 事件：对方落子
  function game_on_opponent_put(message) {
    // {action: "put", x: 2, y: 6 }
    // ui show stone
    UI.set_stone_opponent(message.body.x, message.body.y)

  }

  // 事件：游戏结束
  function game_on_game_end(message) {
    // {"action":"end","winner":"white"}
    UI.isGameEnd = true
    UI.handleGameEnd()
    UI.winner_color = UI.server_stone_color_to_const(message.body.winner)

  }

  // 重置房间
  function game_reset() {
    const action = 'reset'
    eb.send(`game.player.${my_uuid}.s`, {action: action});
  }

  const actionHandlers = {
    "to put": game_on_self_to_put_stone,
    "opponent to put": game_on_opponent_to_put_stone,
    "put": game_on_opponent_put,
    "end": game_on_game_end,
    "opponent nickname": game_on_opponent_nickname_updated,
  }

  const recvMessageHandler = (error, message) => {
    console.log('received a message: ' + JSON.stringify(message) + '  ' + JSON.stringify(error))
    // UI.$notify({
    //   title: `${message.address} recv from server 👇`,
    //   dangerouslyUseHTMLString: true,
    //   message: JSON.stringify({error}) + '<br><b>' + JSON.stringify(message.body) + '</b>',
    //   duration: 0,
    // });


    if (message && message.body) {
      const isActionCatchByHandler = Object.keys(actionHandlers).some((key) => {
        if (key === message.body.action) {
          actionHandlers[key](message)
          return true
        }
      })
      if (!isActionCatchByHandler) {
        console.log('Uncatch action:', message.body.action)
      }
    }
    else if (error) {
      // TODO
    }
  }


  const v = {
    name: 'Home',
    data: () => {
      return {
        gameServer: "//111.231.251.93:8080/eventbus",
        eb: eb,
        isConnected: false,
        isInRoom: false,
        isGameStarted: false,
        isGameEnd: false,
        whoseTurn: null,

        my_uuid: null,
        room: null,
        winner_color: null,

        board: {
          size: 10,
          stones: [],
        },

        countdown_timer: null,
        countdown_number: 0,

        players: {
          self: null,
          self_color: null,
          opponent: null,
          opponent_color: null,
        },

        form_room: {
          room_id: ''
        },
        form_room_rules: {
          room_id: [
            {required: true, message: '请输入房间暗号', trigger: 'submit'},
            {min: 2, max: 128, message: '暗号须在 2 到 128 个字符之间', trigger: 'blur'}
          ],
        },
        loading_form_room: false,

        form_nickname: {
          name: ''
        },
        form_nickname_rules: {
          name: [
            {required: true, message: '请输入昵称', trigger: 'blur'},
            {min: 0, max: 20, message: '昵称须少于 20 个字符', trigger: 'blur'}
          ],
        },

        dialogForm_nickname_visible: false,

        state_init: null
      }
    },
    computed: {
      board_class() {
        return this.board.stones.map((value, idx) => {
          let a = ["board-slot"]
          if (value === kBlackStone) {
            a.push("black-stone")
          }
          else if (value === kWhiteStone) {
            a.push("white-stone")
          }
          else {
            a.push("none-stone")
          }
          // console.log(idx, value, a)
          return a
        })
      },
      board_css_self_stone_background () {
        let file_name, value
        file_name = value = null

        if (this.players.self_color === kWhiteStone) {
          file_name = 'whiteStone'
        }
        else if (this.players.self_color === kBlackStone) {
          file_name = 'blackStone'
        }

        if (file_name) {
          value = `url("/static/${file_name}.png")`
        }

        return value
      },
      avatar_class() {
         const v2class = (value) => {
          let a = []
          if (value === kBlackStone) {
            a.push("avatar-black")
          }
          else if (value === kWhiteStone) {
            a.push("avatar-white")
          }
          // console.log(idx, value, a)
          return a
        }
        return {
          self: v2class(this.players.self_color),
          opponent: v2class(this.players.opponent_color),
        }
      },
      UI_string_my_color() {
        return this.color_const_to_UI_string(this.players.self_color)
      },
      UI_string_opponent_color() {
        return this.color_const_to_UI_string(this.players.opponent_color)
      },
      UI_string_winner() {
        return {
          stone: this.color_const_to_UI_string(this.winner_color) + '棋',
          role: this.winner_color === this.players.self_color ? '我方' : '对方',
        }
      },
      UI_string_whoseTurn() {
        if (this.whoseTurn === 'self') return '我方'
        else if (this.whoseTurn === 'opponent') return '对方'
        else return '...'
      },
    },
    methods: {
      handleStoneSlotClick(cell_idx) {
        // check if game end
        if (!this.isGameStarted || this.isGameEnd) {
          console.log('game is not going on')
          return;
        }

        // check if self turn
        if (this.whoseTurn !== 'self') {
          console.log('not self turn')
          return
        }

        // check if to put at an empty slot
        if (this.board.stones[cell_idx] !== 0) {
          // the slot has been taken by stone.
          return
        }

        // send
        const [x, y] = this.cell_idx_to_xy(cell_idx)

        game_put_stone(x, y, () => {
          // this.$message({
          //   showClose: true,
          //   message: `落子 (${x},${y})`,
          //   type: 'success'
          // });

        })
      },
      handleEnterRoomClick() {
        if (this.form_room.room_id === '') {
          return
        }
        this.loading_form_room = true
        // this.isInRoom = true
        game_enter_room(this.form_room.room_id, (error, message) => {
          const isRoomFull = (message && message.body.color === 'none')
          if (error || isRoomFull) {
            // 进入房间失败
            this.isInRoom = false
            this.room_id = null
            this.$set(this.players, 'self_color', null)
            // 人员已满
            if (isRoomFull) {
              this.$message({
                showClose: true,
                message: '进入房间失败，人员已满',
                type: 'info'
              });
            }
          }
          else if (message) {
            // 进入房间成功
            this.isInRoom = true
            this.room_id = this.form_room.room_id
            this.$set(this.players, 'self_color', this.server_stone_color_to_const(message.body.color))
            this.board.stones.fill(0)
            this.$set(this.board, 'stones', this.board.stones)
            this.board.stones.splice(this.board.stones.length)
          }

          this.loading_form_room = false

        })
      },
      handleLeaveRoomClick() {
        game_leave_room(this.form_room.room_id)
        this.isInRoom = false
        this.isGameEnd = this.isGameStarted = false
        this.countdown_stop()
        this.whoseTurn = null
        this.winner_color = null
        this.room_id = null

        this.$set(this.players, 'self_color', null)
        this.$set(this.players, 'opponent', null)
        this.$set(this.players, 'opponent_color', null)

        this.board.stones.fill(0)
        this.board.stones.splice(this.board.stones.length)  // invoke change detect
      },
      handleChangeNicknameClick () {
        const new_nickname = this.form_nickname.name
        // check if nickname changed
        if (new_nickname !== this.players.self) {
          game_update_nickname(new_nickname)
          // no callback, update immediately
          this.players.self = new_nickname
          // this.$set(this.players, 'self', new_nickname)
        }
        // close dialog
        this.dialogForm_nickname_visible = false
      },
      handleAgainClick () {
        // 再来一局
        this.isGameStarted = false
        this.isGameEnd = false
        this.whoseTurn = null
        this.winner_color = null
        this.board.stones.fill(0)
        this.board.stones.splice(this.board.stones.length)  // invoke change detect
        game_reset()
      },
      handleSubmitForm(formName, submitHandler) {
        this.$refs[formName].validate((valid) => {
          if (valid) {
            submitHandler()
          }
        });
      },
      handleResetForm(formName) {
        this.$refs[formName].resetFields();
      },
      set_stone(cell_idx, color_const) {
        console.log('set stone', {cell_idx, color_const})
        if (color_const === undefined || color_const == null) {
          throw 'invalid stone color'
        }
        if (this.board.stones[cell_idx] !== 0) {
          return
        }
        this.board.stones.splice(cell_idx, 1, color_const);
      },
      set_stone_self(x, y) {
        this.set_stone(this.xy_to_cell_idx(x, y), this.players.self_color)
      },
      set_stone_opponent(x, y) {
        this.set_stone(this.xy_to_cell_idx(x, y), this.players.opponent_color)
      },
      connect() {
        eb = new EventBus(this.gameServer);
        this.eb = eb
        eb.onopen = () => {
          console.log('opened')
          this.isConnected = true
          this.$message({
            showClose: true,
            message: '连接服务器成功',
            type: 'success'
          });

          // eb.registerHandler('game.player.s', function(error, message) {
          //     UI.$notify({
          //         title: `${message.address} recv from server 👇`,
          //         dangerouslyUseHTMLString: true,
          //         message: JSON.stringify({error}) + '<br><b>' + JSON.stringify(message.body) + '</b>',
          //         duration: 0,
          //     });
          //     console.log('received a message: ' + JSON.stringify(message) + '  ' + JSON.stringify(error));
          // });

          eb.send('game.player.s', {action: 'new uuid'}, {}, (error, message) => {
            debug_notify_communication('new uuid', error, message)

            // {"action":"new uuid","uuid":"1b0964bb-4755-4b9f-8835-9a5d0d7ccfcf","nickname":"player 25"}}}
            if (message) {
              my_uuid = this.my_uuid = message.body.uuid
              eb.registerHandler('game.player.' + this.my_uuid + '.b', {}, recvMessageHandler);
              game__heartbeat()
              game_heartbeat_start()
              this.players.self = message.body.nickname

              this.$message({
                showClose: true,
                message: '欢迎来到五子棋，您的 ID 为 ' + this.players.self,
                type: 'success'
              });

              // const new_nickname = 'ynyyn'
              // game_update_nickname(new_nickname)
              // this.players.self = new_nickname;
            }
          });
        }

        eb.onclose = (param) => {
          console.log('eventbus closed', param)
          if (param.code === 1000) {
            // normal closure
            this.$message({
              showClose: true,
              message: '与服务器的连接已关闭',
            });
          } else {
            this.$message({
              showClose: true,
              message: param.reason,
              type: 'error'
            });
          }
          this.handleConnectionClosed()
        }
        eb.enableReconnect(true);
      },
      disconnect() {
        if (eb !== null) {
          this.handleBeforeDisconnect()
          eb.close()
          this.eb = eb = null
        }
      },
      handleBeforeDisconnect() {
        game_heartbeat_stop()
      },
      handleConnectionClosed() {
        game_heartbeat_stop()
        this.isConnected = false
      },
      handleGameEnd() {
        if (this.countdown_timer !== null && this.countdown_number <= 1) {
          // 说明是超时导致结束
          this.countdown_number = 0
        }
        this.countdown_stop()
      },

      countdown__tick() {
        this.countdown_number -= 1
        if (this.countdown_number <= 0) {
          this.countdown_stop()
        }
      },

      countdown_start(start_number) {
        if (typeof start_number === 'number') {
          this.countdown_number = start_number
        }
        this.countdown_stop()
        this.countdown_timer = setInterval(this.countdown__tick, 1000)
        console.log('start countdown', this.countdown_timer)
      },

      countdown_stop() {
        console.log('stop countdown', this.countdown_timer)
        if (this.countdown_timer === null) {
          return
        }
        clearInterval(this.countdown_timer)
        this.countdown_timer = null
      },

      // init
      save_init_state() {
        this.state_init = JSON.parse(JSON.stringify(this.$data))
      },
      reset_state() {
      },

      // helpful funciton
      server_stone_color_to_const(color) {
        // color = 'black' | 'white'
        if (color === 'black') {
          return kBlackStone
        }
        else if (color === 'white') {
          return  kWhiteStone
        }
        else {
          throw `Unknown stone color <${color}>`
        }
      },
      color_const_to_UI_string(color_const) {
        if (color_const === kBlackStone) {
          return '黑'
        }
        else if (color_const === kWhiteStone) {
          return '白'
        }
        else {
          return color_const
        }
      },

      cell_idx_to_xy(cell_idx) {
        return [parseInt(cell_idx / this.board.size), cell_idx % this.board.size]
      },

      xy_to_cell_idx(x, y) {
        return x * this.board.size + y
      },
    },
    watch: {
      players: {
        handler: () => {},
        deep: true//对象内部的属性监听，也叫深度监听
      },
      'players.self_color': function (new_color_const) {
        // console.log("self_color update", new_color_const)
        // console.log(new_color_const === kWhiteStone, new_color_const === kBlackStone)

        // document.body.classList.remove('black-stone-body')
        // document.body.classList.remove('white-stone-body')

        if (new_color_const === kWhiteStone) {
          // this.players.opponent_color = kBlackStone
          this.$set(this.players, 'opponent_color', kBlackStone)
          // document.body.classList.add('white-stone-body')
        }
        else if (new_color_const === kBlackStone) {
          // this.players.opponent_color = kWhiteStone
          this.$set(this.players, 'opponent_color', kWhiteStone)
          // document.body.classList.add('black-stone-body')
        }
        else {
          if (new_color_const) {
            throw 'Unknown color'
          }
        }
      },
      'players.self': function (new_nickname) {
        this.$set(this.form_nickname, 'name', new_nickname)
      },
    },

    created() {
      if (eb !== null) {
        this.disconnect()
      }
      UI = this
      this.board.stones = new Array(this.board.size * this.board.size).fill(0)
      this.save_init_state()
    },

    mounted() {
      this.connect()
      // for debug purpose
      // this.board.stones.splice(10, 1, kWhiteStone);
      // this.board.stones.splice(15, 1, kBlackStone);
      // this.board.stones[10] = kWhiteStone;
      // this.board.stones[15] = kBlackStone;
    },

    beforeDestroy() {
      if (this.room_id !== null) {
         game_leave_room(this.room_id)
      }
      this.disconnect()
      my_uuid = null
      UI = null
    },

  }

  let UI = Object.assign({}, {...v, ...v.data()})
  export default v

</script>

<style scoped>


  .board-container {
    margin: 0 auto;

    display: grid;
    grid-template-columns: repeat(10, minmax(0, 1fr));
    grid-template-rows: repeat(10, minmax(0, 1fr));
    grid-auto-rows: 1fr;
    grid-gap: 2px;

    padding: 2px;
    background-color: #e9cf97;
    max-width: 600px;
  }

  .board-slot {
    color: #fefefe;
    background-color: rgb(211, 168, 76);
    display: flex;
    align-items: stretch;
    /*outline-color: transparent;*/
    /*transition: outline-color .1s ease-out;*/

    user-select: none;
  }

  .board-slot > div {
    color: rgba(255, 255, 255, 0.6);
  }

  /* ratio box: trick to keep container ratio as 1:1 (square) */
  .board-slot::before {
    content: "";
    display: block;
    height: 0;
    width: 0;
    padding-bottom: 100%;
  }

  /* ratio box: trick to make sure ratio container (height) won't be extent by content */
  .board-slot::after {
    clear: left;
    content: " ";
    display: table;
  }

  .board-slot:hover {
    outline: 1px solid rgba(255, 255, 255, 0.75);
  }

  .board-slot.black-stone {
    background-color: #555;
    background-image: url("/static/blackStone.png");
    background-size: 100% 100%;
    background-repeat: no-repeat;
  }

  .board-slot.white-stone {
      background-color: #fefefe;
      background-image: url("/static/whiteStone.png");
      background-size: 100% 100%;
      background-repeat: no-repeat;
  }

  .board-slot.white-stone > div {
      color: rgba(0, 0, 0, 0.3);
  }

  .board-slot > div {
      flex-grow: 1;
      position: relative;
      z-index: 0;
  }

  /*.board-slot.none-stone:hover > div {*/
  /*    background-image: url("/static/whiteStone.png");*/
  /*    !*background-size: 100% 100%;*!*/
  /*    background-size: cover;*/
  /*    background-repeat: no-repeat;*/
  /*}*/

  /* 未落子的棋子位 */
  .board-slot.none-stone {
    cursor: pointer;
  }

  .board-slot.none-stone > div::before {
      content: "";
      display: block;
      /*background: url("/static/whiteStone.png")*/
      background: var(--self-stone-background);
      background-repeat: no-repeat;
      background-size: cover;
      opacity: 0;
      top: 0;
      left: 0;
      height: 100%;
      width: 100%;
      position: absolute;
      transition: opacity 0.05s ease-out 0.02s;
      z-index: -1;
  }

  .board-slot.none-stone:hover > div::before{
      opacity: 0.6;
  }

  .avatar-white {
      background-color: #fdfdfd;
      box-shadow: inset 2px -2px 10px rgba(0,0,0,0.3), -1px 1px 3px rgba(0,0,0,0.8);
      color: #666;
  }

  .avatar-black {
      background-color: #5d5d5d;
      box-shadow: inset 2px -2px 10px rgba(0,0,0,0.7), -1px 1px 3px rgba(0,0,0,0.8);
  }




</style>

<style>
  .card-wrapper .el-card__body {
      padding: 5px;
  }

  td {
      border: solid 2px;
  }


  body {
      /*background: linear-gradient(rgba(255,255,255,0.7), rgba(255,255,255,0.7)), rgb(211, 168, 76);*/
      transition: background 0.3s ease;
      /*background-repeat: no-repeat;*/
      background: linear-gradient(rgba(255,255,255,0.7), rgba(255,255,255,0.7)), url("/static/whiteStone.png") repeat 0 0;
      background-attachment: fixed;
  }

  .black-stone-body {
      background: linear-gradient(rgba(255,255,255,0.7), rgba(255,255,255,0.7)), url("/static/blackStone.png") repeat 0 0;
      background-attachment: fixed;
  }

  .white-stone-body {
      background: linear-gradient(rgba(255,255,255,0.7), rgba(255,255,255,0.7)), url("/static/whiteStone.png") repeat 0 0;
      background-attachment: fixed;
  }

  /*body::after {*/
  /*    content: "";*/
  /*    !*background: url("/static/whiteStone.png"), url("/static/blackStone.png");*!*/
  /*    !*background-repeat: no-repeat;*!*/
  /*    opacity: 0.5;*/
  /*    top: 0;*/
  /*    left: 0;*/
  /*    bottom: 0;*/
  /*    right: 0;*/
  /*    position: absolute;*/
  /*    z-index: -1;*/
  /*}*/

  #dialog-nickname .el-dialog__body {
      padding-top: 5px !important;
      padding-bottom: 0 !important;

  }
</style>