SPEEDUP_STAGES = [1, 2, 3, 6, 12]
module Input
  unless defined?(update_KGC_ScreenCapture)
    class << Input
      alias update_KGC_ScreenCapture update
    end
  end

  $GameSpeed = 0
  $tsprite = BitmapSprite.new(Graphics.width, 38)
  $tsprite.x        = 0
  $tsprite.y        = 0
  $tsprite.z        = 999998
  $tsprite.opacity  = 224
  T_COLOR           = Color.new(255, 255, 255)
  S_COLOR           = Color.new(0, 0, 0)
  
  def self.update
    $mouse.update if defined?($mouse) && $mouse
    update_KGC_ScreenCapture
    if trigger?(Input::F7)
      pbDebugF7
    end
    
    if os_family == "windows" && Input.trigger?(Input::ALT)
      $tsprite.bitmap.clear
      if Input.press?(Input::CTRL)
        $GameSpeed = 0
        Graphics.frame_rate = 40
      else
        $GameSpeed += 1
        $GameSpeed = 0 if $GameSpeed >= SPEEDUP_STAGES.size
        mult = ($GameSpeed > 2) ? 3 : SPEEDUP_STAGES[$GameSpeed]
        Graphics.frame_rate = 40 * mult
        if $GameSpeed > 0
          text_mult = SPEEDUP_STAGES[$GameSpeed]
          textPositions = [
            [_INTL("{1}×", text_mult), Graphics.width, 0, 1, T_COLOR, S_COLOR]
          ]
          pbSetSmallFont($tsprite.bitmap)
          pbDrawTextPositions($tsprite.bitmap,textPositions)
        end
      end
    end
  end
end