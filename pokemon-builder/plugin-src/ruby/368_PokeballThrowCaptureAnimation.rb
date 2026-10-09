#===============================================================================
# Shows the player's Poké Ball being thrown to capture a Pokémon
#===============================================================================
class PokeballThrowCaptureAnimation < PokeBattle_Animation
  include PokeBattle_BallAnimationMixin

  def initialize(sprites,viewport,
                 ballType,numShakes,critCapture,battler,showingTrainer)
    @ballType       = ballType
    @success        = numShakes >= 4
    @numShakes      = (critCapture && numShakes > 0) ? 1 : numShakes
    @critCapture    = critCapture
    @battler        = battler
    @showingTrainer = showingTrainer    # Only true if a Safari Zone battle
    @shadowVisible  = sprites["shadow_#{battler.index}"].visible
    @trainer        = battler.battle.pbPlayer
    super(sprites,viewport)
  end

  def createProcesses
    # Calculate start and end coordinates for battler sprite movement
    batSprite = @sprites["pokemon_#{@battler.index}"]
    shaSprite = @sprites["shadow_#{@battler.index}"]
    traSprite = @sprites["player_1"]
    ballPos = PokeBattle_SceneConstants.pbBattlerPosition(@battler.index,batSprite.sideSize)
    battlerStartX = batSprite.x
    battlerStartY = batSprite.y
    ballStartX = -6
    ballStartY = 246
    ballMidX   = 0   # Unused in arc calculation
    ballMidY   = 78
    ballEndX   = ballPos[0]
    ballEndY   = 112
    ballGroundY = ballPos[1]-4
    # Set up Poké Ball sprite
    ball = addBallSprite(ballStartX,ballStartY,@ballType)
    ball.setZ(0,batSprite.z+1)
    @ballSpriteIndex = (@success) ? @tempSprites.length-1 : -1
    # Set up trainer sprite (only visible in Safari Zone battles)
    if @showingTrainer && traSprite
      if traSprite.bitmap.width>=traSprite.bitmap.height*2
        trainer = addSprite(traSprite,PictureOrigin::Bottom)
        # Trainer animation
        ballStartX, ballStartY = trainerThrowingFrames(ball,trainer,traSprite)
      end
    end
    delay = ball.totalDuration   # 0 or 7
    # Poké Ball arc animation
    ball.setSE(delay,"Battle throw")
    createBallTrajectory(ball,delay,16,
       ballStartX,ballStartY,ballMidX,ballMidY,ballEndX,ballEndY)
    ball.setZ(9,batSprite.z+1)
    ball.setSE(delay+16,"Battle ball hit")
    # Poké Ball opens up
    delay = ball.totalDuration+6
    ballOpenUp(ball,delay,@ballType,true,false)
    # Set up battler sprite
    battler = addSprite(batSprite,PictureOrigin::Bottom)
    # Poké Ball absorbs battler
    delay = ball.totalDuration
    ballBurstCapture(delay,ballEndX,ballEndY,@ballType)
    delay = ball.totalDuration+4
    # NOTE: The Pokémon does not change color while being absorbed into a Poké
    #       Ball during a capture attempt. This may be an oversight in HGSS.
    battler.setSE(delay,"Battle jump to ball")
    battler.moveXY(delay,5,ballEndX,ballEndY)
    battler.moveZoom(delay,5,0)
    battler.setVisible(delay+5,false)
    if @shadowVisible
      # Set up shadow sprite
      shadow = addSprite(shaSprite,PictureOrigin::Center)
      # Shadow animation
      shadow.moveOpacity(delay,5,0)
      shadow.moveZoom(delay,5,0)
      shadow.setVisible(delay+5,false)
    end
    # Poké Ball closes
    delay = battler.totalDuration
    ballSetClosed(ball,delay,@ballType)
    ball.moveTone(delay,3,Tone.new(96,64,-160,160))
    ball.moveTone(delay+5,3,Tone.new(0,0,0,0))
    # Poké Ball critical capture animation
    delay = ball.totalDuration+3
    if @critCapture
      ball.setSE(delay,"Battle ball shake")
      ball.moveXY(delay,1,ballEndX+4,ballEndY)
      ball.moveXY(delay+1,2,ballEndX-4,ballEndY)
      ball.moveXY(delay+3,2,ballEndX+4,ballEndY)
      ball.setSE(delay+4,"Battle ball shake")
      ball.moveXY(delay+5,2,ballEndX-4,ballEndY)
      ball.moveXY(delay+7,1,ballEndX,ballEndY)
      delay = ball.totalDuration+3
    end
    # Poké Ball drops to the ground
    for i in 0...4
      t = [4,4,3,2][i]   # Time taken to rise or fall for each bounce
      d = [1,2,4,8][i]   # Fraction of the starting height each bounce rises to
      delay -= t if i==0
      if i>0
        ball.setZoomXY(delay,100+5*(5-i),100-5*(5-i))   # Squish
        ball.moveZoom(delay,2,100)                      # Unsquish
        ball.moveXY(delay,t,ballEndX,ballGroundY-(ballGroundY-ballEndY)/d)
      end
      ball.moveXY(delay+t,t,ballEndX,ballGroundY)
      ball.setSE(delay+2*t,"Battle ball drop",100-i*7)
      delay = ball.totalDuration
    end
    battler.setXY(ball.totalDuration,ballEndX,ballGroundY)
    # Poké Ball shakes
    delay = ball.totalDuration+12
    [@numShakes,3].min.times do |i|
      ball.setSE(delay,"Battle ball shake")
      ball.moveXY(delay,2,ballEndX-2*(4-i),ballGroundY)
      ball.moveAngle(delay,2,5*(4-i))   # positive means counterclockwise
      ball.moveXY(delay+2,4,ballEndX+2*(4-i),ballGroundY)
      ball.moveAngle(delay+2,4,-5*(4-i))   # negative means clockwise
      ball.moveXY(delay+6,2,ballEndX,ballGroundY)
      ball.moveAngle(delay+6,2,0)
      delay = ball.totalDuration+8
    end
    if @success
      # Pokémon was caught
      ballCaptureSuccess(ball,delay,ballEndX,ballGroundY)
    else
      # Poké Ball opens
      ball.setZ(delay,batSprite.z-1)
      ballOpenUp(ball,delay,@ballType,false)
      ballBurst(delay,ballEndX,ballGroundY,@ballType)
      ball.moveOpacity(delay+2,2,0)
      # Battler emerges
      col = getBattlerColorFromBallType(@ballType)
      col.alpha = 255
      battler.setColor(delay,col)
      battlerAppear(battler,delay,battlerStartX,battlerStartY,batSprite,col)
      if @shadowVisible
        shadow.setVisible(delay+5,true)
        shadow.setZoom(delay+5,100)
        shadow.moveOpacity(delay+5,10,255)
      end
    end
  end

  def dispose
    if @ballSpriteIndex>=0
      # Capture was successful, the Poké Ball sprite should stay around after
      # this animation has finished.
      @sprites["captureBall"] = @tempSprites[@ballSpriteIndex]
      @tempSprites[@ballSpriteIndex] = nil
    end
    super
  end
end