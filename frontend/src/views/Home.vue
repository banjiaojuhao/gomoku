<template>
  <div class="home">
<!--    <img alt="Vue logo" src="../assets/logo.png">-->
<!--    <div>-->
<!--      <p>-->
<!--        If Element is successfully added to this project, you'll see an-->
<!--        <code v-text="'<el-button>'"></code>-->
<!--        below-->
<!--      </p>-->
<!--      <el-button>el-button</el-button>-->
<!--    </div>-->

<el-card class="card-wrapper" style="max-width: 600px;margin: 0 auto; padding: 15px; ">
    <div style="display: flex;
     flex-direction: column; justify-content: center; ">

        <el-row :gutter="20">
            <el-col :span="12">
                <el-avatar>user</el-avatar>
                <p>Player1 {{ players.self }}</p>
            </el-col>
            <el-col :span="12" style="display: flex; justify-content: center">
                <el-avatar>user</el-avatar>
                <p>Player2 {{ players.opposite }}</p>
            </el-col>
        </el-row>

        <el-row style="margin: 0.5em 0; text-align: center">
            <el-col>
                <el-button type="success" round>准备游戏</el-button>
            </el-col>
        </el-row>



      <div><p>hhh</p></div>

        <div>
            <div class="board-container">
                <div :class="board_class[cell_idx]"
                     v-for="(cell, cell_idx) in this.board.stones" :key="cell_idx" @click="set_stone(cell_idx)">
                    <div style="display: flex; justify-content: center; align-items: center">
                        <div>{{ cell_idx }}</div>
                    </div>

                </div>
            </div>
        </div>


        <div v-if="false">
            <table style="border-collapse: collapse">
                <tr v-for="x in board.size" :key="x">
                    <td v-for="y in board.size" :key="y">{{x}} {{y}}</td>

                </tr>
            </table>
        </div>
    </div>
</el-card>
  </div>
</template>

<script>

const kBlackStone = 1;
const kWhiteStone = 2;

export default {
  name: 'Home',
  data: () => {
    return {
      board: {
        size: 10,
        stones: [],
      },

      players: {
        self: null,
        opposite: null
      },

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
              console.log(idx, value, a)
              return a
          })
      }
    },
  methods: {
    set_stone(cell_idx) {
        if (this.board.stones[cell_idx] !== 0) {
            return
        }
        this.board.stones.splice(cell_idx, 1, kWhiteStone);
    }
  },

  created() {
      this.board.stones = new Array(this.board.size * this.board.size).fill(0)
  },

  mounted() {
      this.board.stones.splice(10, 1, kWhiteStone);
      this.board.stones.splice(15, 1, kBlackStone);
      // this.board.stones[10] = kWhiteStone;
      // this.board.stones[15] = kBlackStone;
  }

}
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
      background: url("/static/whiteStone.png");
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

</style>

<style>
    .card-wrapper .el-card__body {
        padding: 5px;
    }

  td {
      border: solid 2px;
  }


    body {
        /*background-repeat: no-repeat;*/

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
</style>