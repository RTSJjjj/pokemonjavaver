#==============================================================================
# * Scene_Controls
#------------------------------------------------------------------------------
# Shows a help screen listing the keyboard controls.
# Display with:
#      pbEventScreen(ButtonEventScene)
#==============================================================================
class ButtonEventScene < EventScene
  def initialize(viewport = nil)
    super
    Graphics.freeze
    addImage(0, 0, "Graphics/Pictures/intro2")

    for key in @keys
      key.origin = PictureOrigin::Top
    end
    for i in 0...5   # Make everything show (almost) immediately
      @keys[i].setOrigin(0, PictureOrigin::Top)
      @keys[i].setOpacity(0, 255)
    end
    pictureWait   # Update event scene with the changes
    Graphics.transition(20)
    # Go to next screen when user presses C
    onCTrigger.set(method(:pbOnScreen1))
  end

  def pbOnScreen1(scene,*args)
    # End scene
    Graphics.freeze
    scene.dispose
    Graphics.transition(20)
  end
end
