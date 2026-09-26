import { useState } from "react";
import nurseBear from "../assets/Bearnurse.png";
import cloudMessage from "../assets/Cloud.png";

export default function HelpCenter() {
const [isEmergencyOpen, setIsEmergencyOpen] = useState(false);
  return (
    <section className="help-page">
      <div className="help-shell">
        <div className="help-left">
          <h1 className="help-title">Medical Guidance</h1>
          <div className="help-guidance-box">
            <p className="help-guidance-lead">
              This page is here to help you decide when urgent medical help may
              be needed. Please do not wait if symptoms feel severe or suddenly
              get worse.
            </p>
            <p className="help-guidance-subtitle">
              Get urgent medical help now if someone has:</p>
            <ul className="help-guidance-list">
              <li>Trouble breathing or breathing that gets worse quickly</li>
              <li>Severe chest pain or pressure</li>
              <li>Confusion, fainting, collapse, or unresponsiveness</li>
              <li>Heavy bleeding that does not stop</li>
              <li>Seizure, stroke signs, or sudden weakness on one side</li>
              <li>Very high fever with severe weakness or dehydration</li>
            </ul>
            <p className="help-guidance-note">
              This chatbot gives guidance only. It does not replace real medical
              professionals in an emergency.
            </p>
          </div>
          <div className="help-emergency-wrap">
          <div className="help-emergency-machine">
          <button
            type="button"
            className="help-emergency-core"
            onClick={() => setIsEmergencyOpen(true)}>
          <span className="help-emergency-core-main">EMERGENCY</span>
          <span className="help-emergency-core-sub">Tap for urgent info</span>
          </button>
          </div>
        </div>
      </div>
      <div className="help-right">
        <div className="help-character-wrap">
          <div className="help-bubble-wrap">
            <img
              src={cloudMessage} alt="Speech bubble" className="help-bubble-img"/>
        <div className="help-bubble-text">My name is Nurse Bear.<br/>Please read carefully, mate.</div>
    </div>
    <img
      src={nurseBear}
      alt="Nurse Bear"
      className="help-bear-single"
    />
  </div>
</div>
      </div>
      {isEmergencyOpen && (
        <div
          className="help-modal-backdrop"
          onClick={() => setIsEmergencyOpen(false)}>
          <div
            className="help-modal"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="help-emergency-title">
            <button
              type="button"
              className="help-modal-close"
              onClick={() => setIsEmergencyOpen(false)}
              aria-label="Close emergency panel">×</button>
            <h2 id="help-emergency-title" className="help-modal-title">
              Emergency Contact Panel
            </h2>
            <p className="help-modal-sub">
              If there is severe breathing difficulty, chest pain, collapse,
              stroke signs, seizure, or heavy bleeding, get urgent help now.
            </p>
            <div className="help-modal-grid">
              <div className="help-contact-card help-contact-card-danger">
                <div className="help-contact-tag">Life-threatening emergency</div>
                <div className="help-contact-number">999</div>
                <p className="help-contact-text">
                  Call emergency services immediately and ask for an ambulance.
                </p>
              </div>
              <div className="help-contact-card">
                <div className="help-contact-tag">Urgent medical advice</div>
                <div className="help-contact-number">111</div>
                <p className="help-contact-text">
                  Use NHS 111 for urgent help when it is serious but not
                  immediately life-threatening.
                </p>
              </div>
              <div className="help-contact-card help-contact-card-soft">
                <div className="help-contact-tag">Before you call</div>
                <ul className="help-contact-checklist">
                  <li>Say what is happening right now</li>
                  <li>Give the location clearly</li>
                  <li>State the person’s age if known</li>
                  <li>Mention breathing, bleeding, chest pain, or collapse</li>
                  <li>Keep your phone nearby after the call</li>
                </ul>
              </div>
            </div>
            <div className="help-modal-actions">
              <button
                type="button"
                className="help-modal-btn-secondary"
                onClick={() => setIsEmergencyOpen(false)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}