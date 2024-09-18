import { useState } from "react";
import "./Placeholder.css";

export const Placeholder = () => {
  const [displayState, setDisplayState] = useState<"instructions" | "design">(
    "instructions"
  );

  const toggleDisplayState = () => {
    if (displayState === "instructions") {
      setDisplayState("design");
    } else {
      setDisplayState("instructions");
    }
  };
  const buttonLabel =
    displayState === "instructions" ? "design" : "instructions";
  return (
    <div className="Placeholder">
      {displayState === "instructions" ? (
        <div className="Placeholder-instructions">
          <span>Your component will go here</span>
        </div>
      ) : (
        <div className="Placeholder-design"></div>
      )}
      <div className="Placeholder-buttonContainer">
        <button onClick={toggleDisplayState}>
          Click to view {buttonLabel}
        </button>
      </div>
    </div>
  );
};
