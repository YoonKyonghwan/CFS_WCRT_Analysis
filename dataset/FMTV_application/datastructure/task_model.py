from dataclasses import dataclass, field
from typing import Optional

__NAMESPACE__ = "http://www.amalthea.itea2.org/model/1.3.0/central"


@dataclass
class Amalthea:
    class Meta:
        name = "AMALTHEA"
        namespace = "http://www.amalthea.itea2.org/model/1.3.0/central"

    sw_model: Optional[str] = field(
        default=None,
        metadata={
            "name": "swModel",
            "type": "Element",
            "namespace": "",
            "required": True,
        }
    )
    hw_model: Optional[str] = field(
        default=None,
        metadata={
            "name": "hwModel",
            "type": "Element",
            "namespace": "",
            "required": True,
        }
    )
    os_model: Optional[str] = field(
        default=None,
        metadata={
            "name": "osModel",
            "type": "Element",
            "namespace": "",
            "required": True,
        }
    )
    stimuli_model: Optional[str] = field(
        default=None,
        metadata={
            "name": "stimuliModel",
            "type": "Element",
            "namespace": "",
            "required": True,
        }
    )
    constraints_model: Optional[str] = field(
        default=None,
        metadata={
            "name": "constraintsModel",
            "type": "Element",
            "namespace": "",
            "required": True,
        }
    )
    event_model: Optional[str] = field(
        default=None,
        metadata={
            "name": "eventModel",
            "type": "Element",
            "namespace": "",
            "required": True,
        }
    )
    mapping_model: Optional[str] = field(
        default=None,
        metadata={
            "name": "mappingModel",
            "type": "Element",
            "namespace": "",
            "required": True,
        }
    )
