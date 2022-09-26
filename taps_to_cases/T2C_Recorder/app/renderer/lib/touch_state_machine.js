const StateMachine = require('javascript-state-machine');
// import { StateMachine } from 'javascript-state-machine';

let FSM = new StateMachine.factory({

  init: 'ready',
  transitions: [
    { name: 'keyDown', from: ['ready', 'up'], to: 'down' },
    { name: 'touchMove', from: 'down', to: 'move' },
    { name: 'keyUp', from: ['down', 'move'], to: 'up' },
    { name: 'release', from: 'up', to: 'none' },
  ],
  data: {
    duraThresholdOfClick: 600,
    duraThresholdOfSwipe: 300,
    posThreshold: 0.1,
    isLongClickDown: false,
    coordinate: {},
    resolution: {},
    xPositions: [],
    yPositions: [],
    downTime: Date.now(),
    action: {},
  },
  methods: {
    onReady: function (resolution, coordinate) {
      this.resolution = resolution;
      this.coordinate = coordinate;
    },

    onKeyDown: function () {
      //TODO: multiple touch
      this.downTime = Date.now();
      // reset data
      this.xPositions = [];
      this.yPositions = [];
      this.action = {};

      console.log('keydown');
    },

    onTouchMove: function (line) {
      let sections = line.toString().split(' ');
      let eventType = sections[2];
      let eventValue = sections[3];
      let durationUntilNow = Date.now() - this.downTime;
      if (eventType == '0035') {
        let x =
          ((Number('0x' + eventValue) - this.coordinate.xMin) * this.resolution.x) /
          (this.coordinate.xMax - this.coordinate.xMin);
        this.xPositions.push(x);
      }
      if (eventType == '0036') {
        let y =
          ((Number('0x' + eventValue) - this.coordinate.yMin) * this.resolution.y) /
          (this.coordinate.yMax - this.coordinate.yMin);
        this.yPositions.push(y);
      }
      if (durationUntilNow > this.duraThresholdOfClick
        && Math.abs(this.xPositions[0] - this.xPositions[this.xPositions.length - 1]) / this.resolution.x < this.posThreshold
        && Math.abs(this.yPositions[0] - this.yPositions[this.yPositions.length - 1]) / this.resolution.y < this.posThreshold) {
        this.isLongClickDown = true;
      }
    },
    onKeyUp: function () {
      let upTime = Date.now();
      let xDif = this.xPositions[0] - this.xPositions[this.xPositions.length - 1];
      let yDif = this.yPositions[0] - this.yPositions[this.yPositions.length - 1];
      let xMaxDis = Math.abs(xDif);
      let yMaxDis = Math.abs(yDif);
      let duration = upTime - this.downTime;
      if (isNaN(xMaxDis)) {
        xMaxDis = 0;
      }
      if (isNaN(yMaxDis)) {
        yMaxDis = 0;
      }
      if (xMaxDis / this.resolution.x < this.posThreshold && yMaxDis / this.resolution.y < this.posThreshold) {
        if (duration < this.duraThresholdOfClick) {
          this.action = {
            actionType: 'click',
            x: this.xPositions[0],
            y: this.yPositions[0],
          };
        } else {
          this.action = {
            actionType: 'longClick',
            x: this.xPositions[0],
            y: this.yPositions[0],
            args: [duration],
          };
        }
      } else {
        if (duration < this.duraThresholdOfSwipe) {
          let direction;
          if (xMaxDis > yMaxDis) {
            direction = xDif < 0 ? 'right' : 'left';
          } else {
            direction = yDif < 0 ? 'down' : 'up';
          }
          this.action = {
            actionType: 'swipe',
            args: [
              direction,
            ]
          };
        } else {
          if (this.isLongClickDown) {
            this.action = {
              actionType: 'dragAndDrop',
              fromX: this.xPositions[0],
              fromY: this.yPositions[0],
              toX: this.xPositions[this.xPositions.length - 1],
              toY: this.yPositions[this.yPositions.length - 1],
              args: [
                Math.floor(-xDif),
                Math.floor(-yDif),
                duration,
              ]
            };
            this.isLongClickDown = false;
          } else {
            this.action = {
              actionType: 'move',
              fromX: this.xPositions[0],
              fromY: this.yPositions[0],
              toX: this.xPositions[this.xPositions.length - 1],
              toY: this.yPositions[this.yPositions.length - 1],
              args: [
                Math.floor(-xDif),
                Math.floor(-yDif),
                duration,
              ]
            };
          }
        }
      }
      console.log(xMaxDis, yMaxDis, duration);
      console.log('up');
      //get x y action
      // console.log(this.action);
      return this.action;

    },
    onRelease: function () {
      console.log('ready');
    },
  },
});

module.exports.FSM = FSM;
